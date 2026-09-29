package com.dadhawk.auth;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.security.enterprise.credential.Credential;
import jakarta.security.enterprise.credential.UsernamePasswordCredential;
import jakarta.security.enterprise.identitystore.CredentialValidationResult;
import jakarta.security.enterprise.identitystore.IdentityStore;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Called by the container on login. Validates the password against OpenLDAP and returns
 * groups as "COMPANY:role" (e.g. "DENEME_GS_1:viewer"), so the company is not lost.
 */
@ApplicationScoped
public class LdapIdentityStore implements IdentityStore {
    @Inject
    LdapAuth ldap;

    @Override
    public CredentialValidationResult validate(Credential credential) {
        if (!(credential instanceof UsernamePasswordCredential c))
            return CredentialValidationResult.NOT_VALIDATED_RESULT;

        var user = ldap.authenticate(c.getCaller(), c.getPasswordAsString());
        if (user.isEmpty()) return CredentialValidationResult.INVALID_RESULT;

        Set<String> groups = user.get().memberships().stream()
                .map(m -> m.companyId() + ":" + m.role())
                .collect(Collectors.toSet());
        return new CredentialValidationResult(new LdapPrincipal(user.get()), groups);
    }
}
