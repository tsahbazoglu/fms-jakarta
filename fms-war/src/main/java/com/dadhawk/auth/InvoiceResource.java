package com.dadhawk.auth;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bson.Document;

/**
 * Every query is filtered by the session's companyId; every action checks a permission.
 */
@Path("invoices")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class InvoiceResource {
    @Inject
    Db db;

    private com.mongodb.client.MongoCollection<Document> col() {
        return db.db().getCollection("invoices");
    }

    @GET
    public List<Document> list(@Context HttpServletRequest req) {
        Ctx c = Ctx.require(req, "invoices:read");
        return col().find(eq("companyId", c.companyId())).into(new ArrayList<>());
    }

    @POST
    public Response create(Map<String, Object> body, @Context HttpServletRequest req) {
        Ctx c = Ctx.require(req, "invoices:create");
        Document doc = new Document("_id", UUID.randomUUID().toString())
                .append("companyId", c.companyId())          // never taken from the request body
                .append("title", String.valueOf(body.get("title")))
                .append("amount", body.get("amount"))
                .append("createdBy", c.userId());
        col().insertOne(doc);
        return Response.status(201).entity(doc).build();
    }

    @DELETE
    @Path("{id}")
    public Response delete(@PathParam("id") String id, @Context HttpServletRequest req) {
        Ctx c = Ctx.require(req, "invoices:delete");
        long n = col().deleteOne(and(eq("_id", id), eq("companyId", c.companyId()))).getDeletedCount();
        return n == 0 ? Response.status(404).build() : Response.noContent().build();
    }
}
