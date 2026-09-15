package tr.org.tspb.service.lms;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.Document;
import com.mongodb.client.model.Filters;

import tr.org.tspb.service.CommonSrv;
import tr.org.tspb.util.stereotype.MyServices;

/**
 * Distributed Batch Expiration Worker & Scheduler processing expired point lots,
 * deducting balances via TX-EXPIRE entries, and issuing proactive expiry notifications.
 */
@MyServices
@ApplicationScoped
public class LmsExpirationScheduler extends CommonSrv {

    private static final long serialVersionUID = 1L;
    private static final String LMS_DB = "lmsdb";
    private static final String COLLECTION_LOTS = "point_lots";
    private static final String COLLECTION_LEDGER = "transaction_ledger";
    private static final String COLLECTION_MEMBER = "member_profile";

    @Inject
    private LmsOutboxService outboxService;

    /**
     * Executes cursor-paginated point expiration processing.
     */
    public int processExpiredPointLots() {
        Date now = new Date();
        List<Document> expiredLots = mongoDbUtil.find(LMS_DB, COLLECTION_LOTS,
                Filters.and(
                        Filters.lte("expirationDate", now),
                        Filters.gt("remainingPoints", 0.0)
                ));

        if (expiredLots == null || expiredLots.isEmpty()) {
            return 0;
        }

        int processedCount = 0;
        for (Document lotDoc : expiredLots) {
            String lotId = lotDoc.getString("lotId");
            String memberCode = lotDoc.getString("memberCode");
            double pointsToExpire = lotDoc.getDouble("remainingPoints");

            if (pointsToExpire <= 0) continue;

            // 1. Zero out lot balance
            mongoDbUtil.updateOne(LMS_DB, COLLECTION_LOTS, Filters.eq("lotId", lotId),
                    new Document("$set", new Document("remainingPoints", 0.0)));

            // 2. Insert TX-EXPIRE entry into ledger
            String txId = "TX-EXPIRE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            Document expireEntry = new Document("transactionId", txId)
                    .append("memberCode", memberCode)
                    .append("type", "EXPIRE")
                    .append("points", -pointsToExpire)
                    .append("pointLotId", lotId)
                    .append("createdDate", now);

            mongoDbUtil.insertOne(LMS_DB, COLLECTION_LEDGER, expireEntry);

            // 3. Update member profile balance
            Document member = mongoDbUtil.findOne(LMS_DB, COLLECTION_MEMBER, Filters.eq("memberCode", memberCode));
            if (member != null) {
                Double balance = member.getDouble("pointsBalance");
                if (balance == null) balance = 0.0;
                double newBalance = Math.max(0.0, balance - pointsToExpire);
                mongoDbUtil.updateOne(LMS_DB, COLLECTION_MEMBER, Filters.eq("memberCode", memberCode),
                        new Document("$set", new Document("pointsBalance", newBalance)));
            }

            // 4. Publish Outbox Event
            outboxService.publishEvent("PointsExpired", new Document("memberCode", memberCode)
                    .append("pointsExpired", pointsToExpire)
                    .append("lotId", lotId));

            processedCount++;
        }

        logger.info("LMS Expiration Batch Complete: Expired [{}] point lots", processedCount);
        return processedCount;
    }

    /**
     * Identifies point lots expiring in T-7 days and publishes proactive warnings.
     */
    public int notifyExpiringSoon() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, 7);
        Date targetDate = cal.getTime();

        List<Document> warningLots = mongoDbUtil.find(LMS_DB, COLLECTION_LOTS,
                Filters.and(
                        Filters.lte("expirationDate", targetDate),
                        Filters.gt("remainingPoints", 0.0),
                        Filters.ne("warnedT7", true)
                ));

        if (warningLots == null || warningLots.isEmpty()) {
            return 0;
        }

        int warnedCount = 0;
        for (Document lotDoc : warningLots) {
            String lotId = lotDoc.getString("lotId");
            String memberCode = lotDoc.getString("memberCode");
            double points = lotDoc.getDouble("remainingPoints");

            outboxService.publishEvent("PointsExpiringSoon", new Document("memberCode", memberCode)
                    .append("expiringPoints", points)
                    .append("expirationDate", lotDoc.getDate("expirationDate")));

            mongoDbUtil.updateOne(LMS_DB, COLLECTION_LOTS, Filters.eq("lotId", lotId),
                    new Document("$set", new Document("warnedT7", true)));

            warnedCount++;
        }

        logger.info("LMS Expiry Warning Batch Complete: Sent [{}] T-7 day warnings", warnedCount);
        return warnedCount;
    }
}
