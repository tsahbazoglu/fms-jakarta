package com.dadhawk.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import java.util.List;

/** Claims from the verified JWT. companyId always comes from here, never from the client's request body. */
public record Ctx(String userId, String companyId, List<String> perms) {

    public static Ctx require(HttpServletRequest req, String permission) {
        String h = req.getHeader("Authorization");
        Ctx c = (h != null && h.startsWith("Bearer ")) ? Jwt.verify(h.substring(7)) : null;
        if (c == null) throw new NotAuthorizedException("Bearer");
        if (permission != null && !c.perms().contains(permission))
            throw new ForbiddenException("Missing permission " + permission);
        return c;
    }
}
