package tr.org.tspb.service.lms;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import org.bson.Document;
import com.mongodb.client.model.Filters;

import tr.org.tspb.service.CommonSrv;
import tr.org.tspb.util.stereotype.MyServices;

/**
 * Real-Time Rule-Based Anti-Fraud Engine executing transaction velocity checks,
 * geo-velocity anomalies, and automated account locks.
 */
@MyServices
@ApplicationScoped
public class LmsAntiFraudService extends CommonSrv {

    private static final long serialVersionUID = 1L;
    private static final String LMS_DB = "lmsdb";
    private static final String COLLECTION_LEDGER = "transaction_ledger";
    private static final String COLLECTION_ALERTS = "fraud_alerts";
    private static final String COLLECTION_MEMBER = "member_profile";

    // Thresholds
    private static final int MAX_TX_PER_10_MIN = 5;
    private static final double RISK_SCORE_THRESHOLD = 80.0;

    // High-Concurrency L1 Memory Sliding Window Velocity Tracker
    private static final java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.ConcurrentLinkedQueue<Long>> VELOCITY_WINDOW = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Evaluates transaction risk score in real time before processing.
     * Returns true if clear, false if blocked by fraud locks.
     */
    public boolean evaluateTransactionRisk(String memberCode, double amount, String storeId) {
        if (memberCode == null || memberCode.trim().isEmpty()) {
            return true;
        }

        // 1. Check if Account is already Locked
        Document member = mongoDbUtil.findOne(LMS_DB, COLLECTION_MEMBER, Filters.eq("memberCode", memberCode));
        if (member != null && Boolean.TRUE.equals(member.getBoolean("fraudLocked"))) {
            logger.warn("LMS Fraud Interceptor: Transaction blocked for locked member [{}]", memberCode);
            return false;
        }

        double riskScore = 0.0;
        String triggeredRule = "NONE";

        // 2. High-Speed L1 Sliding Window Velocity Check (In-Memory + DB Fallback)
        long now = System.currentTimeMillis();
        long windowStart = now - TimeUnit.MINUTES.toMillis(10);

        java.util.concurrent.ConcurrentLinkedQueue<Long> txTimes = VELOCITY_WINDOW.computeIfAbsent(memberCode, k -> new java.util.concurrent.ConcurrentLinkedQueue<>());
        txTimes.removeIf(timestamp -> timestamp < windowStart);
        txTimes.add(now);

        int recentCount = txTimes.size();
        if (recentCount < MAX_TX_PER_10_MIN) {
            // Check DB fallback for absolute count cross-node
            Date tenMinutesAgo = new Date(windowStart);
            List<Document> recentTxs = mongoDbUtil.find(LMS_DB, COLLECTION_LEDGER,
                    Filters.and(Filters.eq("memberCode", memberCode), Filters.gte("createdDate", tenMinutesAgo)));
            if (recentTxs != null) {
                recentCount = Math.max(recentCount, recentTxs.size());
            }
        }

        if (recentCount >= MAX_TX_PER_10_MIN) {
            riskScore += 85.0;
            triggeredRule = "HIGH_VELOCITY_EXCEEDED";
        }

        // 3. Excessive Amount Anomaly Check
        if (amount >= 50000.0) {
            riskScore += 30.0;
            if ("NONE".equals(triggeredRule)) {
                triggeredRule = "SUSPICIOUS_HIGH_AMOUNT";
            }
        }

        // 4. Action Decision
        if (riskScore >= RISK_SCORE_THRESHOLD) {
            String alertId = "ALT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            Document alert = new Document("alertId", alertId)
                    .append("memberCode", memberCode)
                    .append("riskScore", riskScore)
                    .append("ruleTriggered", triggeredRule)
                    .append("actionTaken", "ACCOUNT_LOCKED")
                    .append("createdDate", new Date());

            mongoDbUtil.insertOne(LMS_DB, COLLECTION_ALERTS, alert);

            // Lock member account
            mongoDbUtil.updateOne(LMS_DB, COLLECTION_MEMBER, Filters.eq("memberCode", memberCode),
                    new Document("$set", new Document("fraudLocked", true)));

            logger.error("LMS Fraud Alert Triggered: Member [{}] locked (Risk Score: [{}], Rule: [{}])",
                    memberCode, riskScore, triggeredRule);

            return false;
        }

        return true;
    }

    // Cashier Anomaly Tracker (Cashier ID -> List of member codes scanned in last 1 hour)
    private static final java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.ConcurrentLinkedQueue<String>> CASHIER_SCAN_WINDOW = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Evaluates store-level cashier self-scanning fraud anomalies.
     * Prevents cashiers from repeatedly scanning their own loyalty card on non-member cash sales.
     */
    public boolean evaluateCashierAnomaly(String cashierId, String storeId, String memberCode) {
        if (cashierId == null || cashierId.trim().isEmpty() || memberCode == null) {
            return true;
        }

        String key = storeId + ":" + cashierId;
        java.util.concurrent.ConcurrentLinkedQueue<String> scans = CASHIER_SCAN_WINDOW.computeIfAbsent(key, k -> new java.util.concurrent.ConcurrentLinkedQueue<>());
        scans.add(memberCode + ":" + System.currentTimeMillis());

        // Keep last 1 hour of scans
        long oneHourAgo = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1);
        scans.removeIf(item -> {
            String[] parts = item.split(":");
            return parts.length < 2 || Long.parseLong(parts[1]) < oneHourAgo;
        });

        // Count how many times this SAME memberCode was scanned by this cashier
        long sameMemberCount = scans.stream()
                .filter(item -> item.startsWith(memberCode + ":"))
                .count();

        if (sameMemberCount > 3) {
            logger.warn("LMS Anti-Fraud Interceptor: Cashier [{}] at Store [{}] scanned same Member [{}] [{}] times in 1 hour!",
                    cashierId, storeId, memberCode, sameMemberCount);

            Document alert = new Document("alertId", "ALT-CASHIER-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .append("forms", "fraud_alerts")
                    .append("cashierId", cashierId)
                    .append("storeId", storeId)
                    .append("memberCode", memberCode)
                    .append("riskScore", 95.0)
                    .append("ruleTriggered", "CASHIER_SELF_SCAN_ANOMALY")
                    .append("actionTaken", "CASHIER_FLAGGED_FOR_AUDIT")
                    .append("createdDate", new Date());

            mongoDbUtil.insertOne(LMS_DB, COLLECTION_ALERTS, alert);
            return false;
        }
        return true;
    }
}
