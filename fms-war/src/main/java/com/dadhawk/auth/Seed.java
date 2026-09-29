package com.dadhawk.auth;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.util.List;
import org.bson.Document;

/**
 * Seeds the role -> CRUD permission map. Who has which role in which company lives in LDAP.
 */
@ApplicationScoped
public class Seed {
    @Inject
    Db db;

    void onStart(@Observes @Initialized(ApplicationScoped.class) Object evt) {
        var roles = db.db().getCollection("roles");
        if (roles.countDocuments() > 0) return;
        roles.insertMany(List.of(
                role("admin", "invoices:create", "invoices:read", "invoices:update", "invoices:delete"),
                role("editor", "invoices:create", "invoices:read", "invoices:update"),
                role("viewer", "invoices:read")));
    }

    private static Document role(String name, String... perms) {
        return new Document("_id", name).append("permissions", List.of(perms));
    }
}
