package com.dadhawk.auth;

import static com.mongodb.client.model.Filters.eq;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.bson.Document;

@Path("auth")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {
    private static final long TTL_SECONDS = 3600;
    @Inject LdapAuth ldap;
    @Inject Db db;

    @POST @Path("login")
    public Response login(Map<String, String> body, @Context HttpServletRequest req) {
        if (body == null) return error(400, "Request body required");
        String username = body.get("username");
        String password = body.get("password");
        if (username == null || password == null) return error(400, "Username and password required");
        var lu = ldap.authenticate(username, password);
        if (lu.isEmpty()) return error(401, "Invalid credentials");
        var u = lu.get();
        if (req != null) {
            try {
                if (req.getRemoteUser() != null) {
                    req.logout();
                }
                req.login(u.uid() != null ? u.uid() : username, password);
            } catch (Exception ex) {
                java.util.logging.Logger.getLogger(AuthResource.class.getName())
                        .log(java.util.logging.Level.WARNING, "Container login failed for user " + username, ex);
            }
        }
        Response resp = issue(u.entryUuid(), u.uid(), u.memberships(), body.get("companyId"));
        if (req != null && resp.getEntity() instanceof Map map && map.containsKey("companyId")) {
            String cid = (String) map.get("companyId");
            if (cid != null && !cid.isBlank()) {
                req.getSession(true).setAttribute("jaasLoginName", cid);
            }
        }
        return resp;
    }

    @POST @Path("switch")
    public Response switchCompany(Map<String, String> body, @Context HttpServletRequest req) {
        Ctx cur = Ctx.require(req, null);
        String wanted = body.get("companyId");
        if (wanted == null || wanted.isBlank()) return error(400, "companyId required");
        Response resp = issue(cur.userId(), null, ldap.membershipsByEntryUuid(cur.userId()), wanted);
        if (req != null && resp.getEntity() instanceof Map map && map.containsKey("companyId")) {
            String cid = (String) map.get("companyId");
            if (cid != null && !cid.isBlank()) {
                req.getSession(true).setAttribute("jaasLoginName", cid);
            }
        }
        return resp;
    }

    @POST @Path("select-role")
    public Response selectRole(Map<String, String> body, @Context HttpServletRequest req) {
        if (body != null && body.containsKey("role") && req != null) {
            String role = body.get("role");
            if (role != null && !role.isBlank()) {
                req.getSession(true).setAttribute("jaasLoginName", role.trim());
            }
        }
        return Response.ok(Map.of("status", "ok")).build();
    }

    @GET @Path("me")
    public Ctx me(@Context HttpServletRequest req) { return Ctx.require(req, null); }

    private Response issue(String entryUuid, String uid, List<LdapAuth.Membership> all, String wanted) {
        List<String> companies = all.stream().map(LdapAuth.Membership::companyId).distinct().sorted().toList();
        String cid = (wanted == null || wanted.isBlank())
                ? (companies.isEmpty() ? null : companies.get(0))
                : companies.stream().filter(c -> c.equals(wanted)).findFirst().orElse(null);
        if (cid == null) return error(403, "No role in that company");

        List<String> roleNames = all.stream().filter(m -> m.companyId().equals(cid))
                .map(LdapAuth.Membership::role).distinct().toList();
        var perms = new LinkedHashSet<String>();
        for (String r : roleNames) {
            Document role = db.db().getCollection("roles").find(eq("_id", r)).first();
            if (role != null) perms.addAll(role.getList("permissions", String.class));
        }
        if (perms.isEmpty()) return error(403, "Unknown role " + roleNames);

        var ctx = new Ctx(entryUuid, cid, new ArrayList<>(perms));
        String jwtToken = Jwt.sign(ctx, TTL_SECONDS);
        var out = new LinkedHashMap<String, Object>();
        out.put("token", jwtToken);
        out.put("expiresIn", TTL_SECONDS);
        if (uid != null) out.put("userId", uid);
        out.put("companyId", cid);
        out.put("role", String.join(",", roleNames));
        out.put("permissions", ctx.perms());
        out.put("companies", companies);

        jakarta.ws.rs.core.NewCookie cookie = new jakarta.ws.rs.core.NewCookie.Builder("token")
                .value(jwtToken)
                .path("/")
                .maxAge((int) TTL_SECONDS)
                .httpOnly(false)
                .sameSite(jakarta.ws.rs.core.NewCookie.SameSite.LAX)
                .build();
        jakarta.ws.rs.core.NewCookie casCookie = new jakarta.ws.rs.core.NewCookie.Builder("cas_token")
                .value(jwtToken)
                .path("/")
                .maxAge((int) TTL_SECONDS)
                .httpOnly(false)
                .sameSite(jakarta.ws.rs.core.NewCookie.SameSite.LAX)
                .build();

        return Response.ok(out).cookie(cookie, casCookie).build();
    }

    private static Response error(int status, String msg) {
        return Response.status(status).entity(Map.of("error", msg)).build();
    }
}