package tr.org.tspb.service.lms;

import java.util.Date;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.Document;
import com.mongodb.client.model.Filters;

import tr.org.tspb.service.CommonSrv;
import tr.org.tspb.util.stereotype.MyServices;

/**
 * Service managing API idempotency keys to ensure exact-once execution of point operations.
 */
@MyServices
@ApplicationScoped
public class LmsIdempotencyService extends CommonSrv {

    private static final long serialVersionUID = 1L;
    private static final String LMS_DB = "lmsdb";
    private static final String COLLECTION_IDEMPOTENCY = "lms_idempotency_keys";

    /**
     * Checks if an idempotency key exists. Returns cached response if present.
     */
    public Document getCachedResponse(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            return null;
        }
        Document record = mongoDbUtil.findOne(LMS_DB, COLLECTION_IDEMPOTENCY, Filters.eq("idempotencyKey", idempotencyKey));
        if (record != null && record.containsKey("response")) {
            return (Document) record.get("response");
        }
        return null;
    }

    /**
     * Records a new idempotency key along with execution result.
     */
    public void storeIdempotentResult(String idempotencyKey, String requestPath, Document responseDocument) {
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            return;
        }
        Date now = new Date();
        Document record = new Document("idempotencyKey", idempotencyKey)
                .append("requestPath", requestPath)
                .append("response", responseDocument)
                .append("createdDate", now);

        mongoDbUtil.insertOne(LMS_DB, COLLECTION_IDEMPOTENCY, record);
        logger.info("LMS Idempotency Key stored: [{}] for path [{}]", idempotencyKey, requestPath);
    }
}
