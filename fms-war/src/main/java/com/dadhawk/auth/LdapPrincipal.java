package com.dadhawk.auth;

import jakarta.security.enterprise.CallerPrincipal;
import java.util.List;

/** The logged-in caller, carrying the company + role memberships read from LDAP at login. */
public class LdapPrincipal extends CallerPrincipal {
    private final String entryUuid;
    private final List<LdapAuth.Membership> memberships;

    public LdapPrincipal(LdapAuth.LdapUser u) {
        super(u.uid());
        this.entryUuid = u.entryUuid();
        this.memberships = List.copyOf(u.memberships());
    }

    public String entryUuid() { return entryUuid; }
    public List<LdapAuth.Membership> memberships() { return memberships; }
}
