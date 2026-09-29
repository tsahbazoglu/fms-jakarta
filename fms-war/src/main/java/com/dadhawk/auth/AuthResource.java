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
    @Inject
    LdapAuth ldap;
    @Inject
    Db db;

    /**
     * Body: {"username":"ayse@dadhawk.com","password":"...","companyId":"DENEME_GS_1"}  (companyId optional)
     */
    @POST
    @Path("login")
    public Response login(Map<String, String> body) {
        var lu = ldap.authenticate(body.get("username"), body.get("password"));
        if (lu.isEmpty()) return error(401, "Invalid credentials");
        var u = lu.get();
        return issue(u.entryUuid(), u.uid(), u.memberships(), body.get("companyId"));
    }

    /**
     * Body: {"companyId":"DENEME_KURUM"}. Needs the current Bearer token; no password.
     */
    @POST
    @Path("switch")
    public Response switchCompany(Map<String, String> body, @Context HttpServletRequest req) {
        Ctx cur = Ctx.require(req, null);
        String wanted = body.get("companyId");
        if (wanted == null || wanted.isBlank()) return error(400, "companyId required");
        // Fresh from LDAP, not from the old token, so removed roles stop working here.
        return issue(cur.userId(), null, ldap.membershipsByEntryUuid(cur.userId()), wanted);
    }

    @GET
    @Path("me")
    public Ctx me(@Context HttpServletRequest req) {
        return Ctx.require(req, null);
    }

    /**
     * Picks the company, combines the user's roles in it, maps them to permissions, signs the token.
     */
    private Response issue(String entryUuid, String uid, List<LdapAuth.Membership> all, String wanted) {
        List<String> companies = all.stream().map(LdapAuth.Membership::companyId).distinct().sorted().toList();
        String cid = (wanted == null || wanted.isBlank())
                ? (companies.isEmpty() ? null : companies.get(0))
                : companies.stream().filter(c -> c.equals(wanted)).findFirst().orElse(null);
        if (cid == null) return error(403, "No role in that company");

        List<String> roleNames = all.stream().filter(m -> m.companyId().equals(cid))
                .map(LdapAuth.Membership::role).distinct().toList();
        var perms = new LinkedHashSet<String>();
        for (String r : roleNames) {   // role name -> CRUD permissions (MongoDB)
            Document role = db.db().getCollection("roles").find(eq("_id", r)).first();
            if (role != null) perms.addAll(role.getList("permissions", String.class));
        }
        if (perms.isEmpty()) return error(403, "Unknown role " + roleNames);

        var ctx = new Ctx(entryUuid, cid, new ArrayList<>(perms));   // sub = immutable LDAP entryUUID
        var out = new LinkedHashMap<String, Object>();
        out.put("token", Jwt.sign(ctx, TTL_SECONDS));
        out.put("expiresIn", TTL_SECONDS);
        if (uid != null) out.put("userId", uid);
        out.put("companyId", cid);
        out.put("role", String.join(",", roleNames));
        out.put("permissions", ctx.perms());
        out.put("companies", companies);
        return Response.ok(out).build();
    }

    private static Response error(int status, String msg) {
        return Response.status(status).entity(Map.of("error", msg)).build();
    }
}
