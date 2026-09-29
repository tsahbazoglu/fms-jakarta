package com.dadhawk.auth;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.security.enterprise.authentication.mechanism.http.BasicAuthenticationMechanismDefinition;

/**
 * Turns on HTTP Basic login through Jakarta Security; credentials are checked by LdapIdentityStore.
 */
//@BasicAuthenticationMechanismDefinition(realmName = "dadhawk.com")
//@ApplicationScoped
public class AuthConfig {
}
