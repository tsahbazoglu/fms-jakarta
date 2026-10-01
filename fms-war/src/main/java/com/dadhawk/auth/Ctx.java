package com.dadhawk.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import java.util.List;

/**
 * Claims from the verified JWT. companyId always comes from here, never from the client's request body.
 */
public record Ctx(String userId, String companyId, List<String> perms) {

    public static Ctx require(HttpServletRequest req, String permission) {
        String token = null;
        String h = req != null ? req.getHeader("Authorization") : null;
        if (h != null && h.startsWith("Bearer ")) {
            token = h.substring(7);
        } else if (req != null && req.getCookies() != null) {
            for (jakarta.servlet.http.Cookie c : req.getCookies()) {
                if ("token".equalsIgnoreCase(c.getName())
                        || "cas_token".equalsIgnoreCase(c.getName())
                        || "auth_token".equalsIgnoreCase(c.getName())) {
                    token = c.getValue();
                    break;
                }
            }
        }
        Ctx c = (token != null && !token.isBlank()) ? Jwt.verify(token) : null;
        if (c == null) throw new NotAuthorizedException("Bearer");
        if (permission != null && !c.perms().contains(permission))
            throw new ForbiddenException("Missing permission " + permission);
        return c;
    }
}
