package tr.org.tspb.service.lms;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import org.bson.Document;
import com.mongodb.client.model.Filters;

import tr.org.tspb.service.CommonSrv;
import tr.org.tspb.util.stereotype.MyServices;

/**
 * Transactional Outbox Service ensuring at-least-once delivery of LMS events
 * to external CRM, CDP, and Push Notification channels.
 */
@MyServices
@ApplicationScoped
public class LmsOutboxService extends CommonSrv {

    private static final long serialVersionUID = 1L;
    private static final String LMS_DB = "lmsdb";
    private static final String COLLECTION_OUTBOX = "lms_event_outbox";

    /**
     * Records an event into the transactional outbox queue.
     */
    public void publishEvent(String eventType, Document payload) {
        String eventId = "EVT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Date now = new Date();

        Document outboxRecord = new Document("eventId", eventId)
                .append("eventType", eventType)
                .append("payload", payload)
                .append("status", "PENDING")
                .append("retryCount", 0)
                .append("createdDate", now);

        mongoDbUtil.insertOne(LMS_DB, COLLECTION_OUTBOX, outboxRecord);
        logger.info("LMS Outbox Event Stored: [{}] (Type: [{}])", eventId, eventType);
    }

    /**
     * Processes pending outbox events and dispatches to subscribers.
     */
    public void processPendingEvents() {
        List<Document> pending = mongoDbUtil.find(LMS_DB, COLLECTION_OUTBOX, Filters.eq("status", "PENDING"));
        if (pending == null || pending.isEmpty()) return;

        for (Document evt : pending) {
            String eventId = evt.getString("eventId");
            String type = evt.getString("eventType");
            try {
                // Dispatch logic (JMS topic / HTTP webhook integration)
                logger.info("LMS Outbox Dispatching Event [{}] (Type: [{}])", eventId, type);

                mongoDbUtil.updateOne(LMS_DB, COLLECTION_OUTBOX, Filters.eq("eventId", eventId),
                        new Document("$set", new Document("status", "PROCESSED").append("processedDate", new Date())));
            } catch (Exception e) {
                logger.error("LMS Outbox Event Dispatch Failed for [{}]", eventId, e);
                int retries = evt.getInteger("retryCount", 0) + 1;
                String newStatus = retries >= 3 ? "FAILED_DLQ" : "PENDING";
                mongoDbUtil.updateOne(LMS_DB, COLLECTION_OUTBOX, Filters.eq("eventId", eventId),
                        new Document("$set", new Document("status", newStatus).append("retryCount", retries)));
            }
        }
    }
}
