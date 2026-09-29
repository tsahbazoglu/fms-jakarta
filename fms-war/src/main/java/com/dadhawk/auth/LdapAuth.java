package com.dadhawk.auth;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Optional;
import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapName;

/**
 * LDAP layout:
 * ou=users,BASE                          uid=alice ...
 * ou=companies,BASE / ou=<company> / cn=<role>   (groupOfNames, member: user DNs)
 * A user's company + role come from which role groups list them as a member.
 */
@ApplicationScoped
public class LdapAuth {
    public record Membership(String companyId, String role) {
    }

    public record LdapUser(String uid, String entryUuid, String mail, List<Membership> memberships) {
    }

    private static final String URL = Db.env("LDAP_URL", "ldap://localhost:389"); // use ldaps:// in production
    private static final String BASE = Db.env("LDAP_BASE", "dc=dadhawk,dc=com");
    private static final String SVC_DN = Db.env("LDAP_BIND_DN", "cn=admin," + BASE);
    private static final String SVC_PW = Db.env("LDAP_BIND_PW", "adminpw");

    private static Hashtable<String, Object> env(String dn, String pw) {
        var e = new Hashtable<String, Object>();
        e.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        e.put(Context.PROVIDER_URL, URL);
        e.put(Context.SECURITY_AUTHENTICATION, "simple");
        e.put(Context.SECURITY_PRINCIPAL, dn);
        e.put(Context.SECURITY_CREDENTIALS, pw);
        return e;
    }

    /**
     * 1) find the user's DN, 2) bind as the user, 3) read the user's role groups.
     */
    public Optional<LdapUser> authenticate(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isEmpty())
            return Optional.empty();   // empty password would be an anonymous bind = "success"
        try {
            DirContext svc = new InitialDirContext(env(SVC_DN, SVC_PW));
            try {
                var sc = new SearchControls();
                sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
                sc.setReturningAttributes(new String[]{"uid", "mail", "entryUUID"});
                // {0} is escaped by JNDI, which prevents LDAP injection
                NamingEnumeration<SearchResult> r =
                        svc.search("ou=users," + BASE, "(uid={0})", new Object[]{username}, sc);
                if (!r.hasMore()) return Optional.empty();
                SearchResult sr = r.next();
                String dn = sr.getNameInNamespace();
                var a = sr.getAttributes();

                new InitialDirContext(env(dn, password)).close();   // throws if password is wrong

                return Optional.of(new LdapUser(
                        a.get("uid").get().toString(),
                        a.get("entryUUID").get().toString(),
                        a.get("mail") != null ? a.get("mail").get().toString() : null,
                        memberships(svc, dn)));
            } finally {
                svc.close();
            }
        } catch (AuthenticationException bad) {
            return Optional.empty();
        } catch (NamingException e) {
            throw new IllegalStateException("LDAP error: " + e.getMessage(), e);
        }
    }

    /**
     * Fresh company + role memberships for a user identified by entryUUID (used when switching company).
     */
    public List<Membership> membershipsByEntryUuid(String entryUuid) {
        try {
            DirContext svc = new InitialDirContext(env(SVC_DN, SVC_PW));
            try {
                var sc = new SearchControls();
                sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
                sc.setReturningAttributes(new String[]{"uid"});
                NamingEnumeration<SearchResult> r =
                        svc.search("ou=users," + BASE, "(entryUUID={0})", new Object[]{entryUuid}, sc);
                if (!r.hasMore()) return List.of();
                return memberships(svc, r.next().getNameInNamespace());
            } finally {
                svc.close();
            }
        } catch (NamingException e) {
            throw new IllegalStateException("LDAP error: " + e.getMessage(), e);
        }
    }

    private List<Membership> memberships(DirContext svc, String userDn) throws NamingException {
        var sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
        sc.setReturningAttributes(new String[]{"cn"});
        var out = new ArrayList<Membership>();
        NamingEnumeration<SearchResult> r = svc.search("ou=companies," + BASE,
                "(&(objectClass=groupOfNames)(member={0}))", new Object[]{userDn}, sc);
        while (r.hasMore()) {
            // relative name looks like "cn=admin,ou=acme"
            LdapName n = new LdapName(r.next().getName());
            if (n.size() >= 2)
                out.add(new Membership(n.getRdn(n.size() - 2).getValue().toString(),
                        n.getRdn(n.size() - 1).getValue().toString()));
        }
        return out;
    }
}
