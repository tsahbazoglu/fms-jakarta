package com.dadhawk.auth;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.security.enterprise.SecurityContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import java.util.List;

/**
 * Gives resources the caller's company after a successful container login.
 */
@RequestScoped
public class CompanyContext {
    @Inject
    SecurityContext security;
    @Inject
    HttpServletRequest request;

    public LdapPrincipal caller() {
        return security.getPrincipalsByType(LdapPrincipal.class).stream().findFirst()
                .orElseThrow(() -> new NotAuthorizedException("Basic realm=\"dadhawk.com\""));
    }

    public List<String> companies() {
        return caller().memberships().stream().map(LdapAuth.Membership::companyId).distinct().toList();
    }

    /**
     * Only one company -> used automatically. Several -> the client picks with the X-Company header.
     */
    public String companyId() {
        List<String> mine = companies();
        String wanted = request.getHeader("X-Company");
        if (wanted == null || wanted.isBlank()) {
            if (mine.size() == 1) return mine.get(0);
            throw new BadRequestException("Choose a company with the X-Company header: " + mine);
        }
        return mine.stream().filter(c -> c.equals(wanted)).findFirst()
                .orElseThrow(() -> new ForbiddenException("Not a member of " + wanted));
    }

    public List<String> roles(String companyId) {
        return caller().memberships().stream()
                .filter(m -> m.companyId().equals(companyId)).map(LdapAuth.Membership::role).toList();
    }

    public boolean inRole(String companyId, String role) {
        return security.isCallerInRole(companyId + ":" + role);
    }
}
