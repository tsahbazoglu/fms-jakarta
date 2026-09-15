package tr.org.tspb.service.api;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.bson.Document;

import tr.org.tspb.service.LmsLedgerService;
import tr.org.tspb.service.lms.LmsCartPipeline;
import tr.org.tspb.service.lms.LmsSyncService;
import tr.org.tspb.service.lms.domain.CartItem;

/**
 * Headless JAX-RS REST Gateway exposing Loyalty Management System (LMS) endpoints
 * for mobile apps, POS terminals, and third-party merchant integrations.
 * 
 * @author Antigravity AI / Telman Şahbazoğlu
 */
@Path("lms/v1")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LmsRestGateway {

    @Inject
    private LmsLedgerService ledgerService;

    @Inject
    private LmsCartPipeline cartPipeline;

    @Inject
    private LmsSyncService syncService;

    @GET
    @Path("/status")
    public Response getStatus() {
        Document res = new Document("status", "UP")
                .append("system", "fms-jakarta LMS Gateway")
                .append("version", "1.4.0-EnterpriseReady");
        return Response.ok(res.toJson()).build();
    }

    @POST
    @Path("/pos/calculate")
    public Response calculateCart(String payloadJson) {
        try {
            Document payload = Document.parse(payloadJson);
            String memberCode = payload.getString("memberCode");
            String storeId = payload.getString("storeId");
            List<Document> rawItems = payload.getList("items", Document.class);

            List<CartItem> cartItems = new ArrayList<>();
            if (rawItems != null) {
                for (Document doc : rawItems) {
                    String sku = doc.getString("sku");
                    String name = doc.getString("name");
                    Double unitPrice = doc.getDouble("unitPrice");
                    Integer qty = doc.getInteger("quantity");
                    String cat = doc.getString("category");

                    CartItem item = new CartItem(
                            sku != null ? sku : "DEFAULT-SKU",
                            name != null ? name : "Item",
                            unitPrice != null ? unitPrice : 0.0,
                            qty != null ? qty : 1,
                            cat != null ? cat : "GENERAL"
                    );
                    cartItems.add(item);
                }
            }

            List<String> promocodes = payload.getList("appliedPromocodes", String.class);

            Document result = cartPipeline.calculateCart(memberCode, cartItems, storeId, promocodes);
            return Response.ok(result.toJson()).build();
        } catch (Exception e) {
            Document error = new Document("error", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(error.toJson()).build();
        }
    }

    @POST
    @Path("/pos/sync")
    public Response syncOfflineBatch(String payloadJson) {
        try {
            Document payload = Document.parse(payloadJson);
            String storeId = payload.getString("storeId");
            List<Document> offlineTxs = payload.getList("offlineTransactions", Document.class);

            Document result = syncService.reconcileOfflineBatch(storeId, offlineTxs);
            return Response.ok(result.toJson()).build();
        } catch (Exception e) {
            Document error = new Document("error", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(error.toJson()).build();
        }
    }

    @POST
    @Path("/accrue")
    public Response accruePoints(@HeaderParam("Idempotency-Key") String idempotencyKey, String payloadJson) {
        try {
            Document payload = Document.parse(payloadJson);
            String memberCode = payload.getString("memberCode");
            Double amount = payload.getDouble("amount");
            String orderRef = payload.getString("orderRef");
            String programCode = payload.getString("programCode");
            String category = payload.getString("category");

            Document result = ledgerService.accruePoints(memberCode, amount, orderRef, programCode, category, idempotencyKey);
            return Response.ok(result.toJson()).build();
        } catch (Exception e) {
            Document error = new Document("error", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(error.toJson()).build();
        }
    }

    @POST
    @Path("/redeem")
    public Response redeemPoints(@HeaderParam("Idempotency-Key") String idempotencyKey, String payloadJson) {
        try {
            Document payload = Document.parse(payloadJson);
            String memberCode = payload.getString("memberCode");
            Double pointsToBurn = payload.getDouble("points");
            String rewardRef = payload.getString("rewardRef");

            Document result = ledgerService.redeemPoints(memberCode, pointsToBurn, rewardRef, idempotencyKey);
            return Response.ok(result.toJson()).build();
        } catch (Exception e) {
            Document error = new Document("error", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(error.toJson()).build();
        }
    }

    @POST
    @Path("/hold")
    public Response holdPoints(String payloadJson) {
        try {
            Document payload = Document.parse(payloadJson);
            String memberCode = payload.getString("memberCode");
            Double pointsToHold = payload.getDouble("points");
            String orderRef = payload.getString("orderRef");

            Document result = ledgerService.holdPoints(memberCode, pointsToHold, orderRef);
            return Response.ok(result.toJson()).build();
        } catch (Exception e) {
            Document error = new Document("error", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(error.toJson()).build();
        }
    }

    @POST
    @Path("/capture")
    public Response captureHold(String payloadJson) {
        try {
            Document payload = Document.parse(payloadJson);
            String memberCode = payload.getString("memberCode");
            String holdTxId = payload.getString("holdTxId");

            Document result = ledgerService.captureHold(memberCode, holdTxId);
            return Response.ok(result.toJson()).build();
        } catch (Exception e) {
            Document error = new Document("error", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(error.toJson()).build();
        }
    }

    @POST
    @Path("/release")
    public Response releaseHold(String payloadJson) {
        try {
            Document payload = Document.parse(payloadJson);
            String memberCode = payload.getString("memberCode");
            String holdTxId = payload.getString("holdTxId");

            ledgerService.releaseHold(memberCode, holdTxId);
            Document result = new Document("status", "SUCCESS").append("message", "Hold released");
            return Response.ok(result.toJson()).build();
        } catch (Exception e) {
            Document error = new Document("error", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(error.toJson()).build();
        }
    }

    @GET
    @Path("/balance/{memberCode}")
    public Response getBalance(@PathParam("memberCode") String memberCode) {
        try {
            Document wallet = ledgerService.getMemberWallet(memberCode);
            if (wallet == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new Document("error", "Member not found").toJson()).build();
            }
            return Response.ok(wallet.toJson()).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new Document("error", e.getMessage()).toJson()).build();
        }
    }
}
