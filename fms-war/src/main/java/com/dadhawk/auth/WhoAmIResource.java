package com.dadhawk.auth;

import static com.mongodb.client.model.Filters.eq;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.LinkedHashSet;
import java.util.Map;
import org.bson.Document;

/** Container-managed login demo: curl -u ayse@dadhawk.com:password123 -H 'X-Company: DENEME_GS_1' .../api/whoami */
@Path("whoami")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
public class WhoAmIResource {
    @Inject CompanyContext company;
    @Inject Db db;

    @GET
    public Map<String, Object> whoami() {
        String cid = company.companyId();
        var roles = company.roles(cid);
        var perms = new LinkedHashSet<String>();
        for (String r : roles) {
            Document d = db.db().getCollection("roles").find(eq("_id", r)).first();
            if (d != null) perms.addAll(d.getList("permissions", String.class));
        }
        return Map.of("user", company.caller().getName(), "companyId", cid,
                      "roles", roles, "permissions", perms, "companies", company.companies());
    }
}
