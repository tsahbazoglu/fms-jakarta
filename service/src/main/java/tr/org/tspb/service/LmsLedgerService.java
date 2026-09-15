package tr.org.tspb.service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.Document;
import com.mongodb.client.model.Filters;

import tr.org.tspb.service.lms.LmsIdempotencyService;
import tr.org.tspb.service.lms.domain.PointLot;
import tr.org.tspb.util.stereotype.MyServices;

/**
 * Service managing double-entry immutable point ledger transactions, FIFO expiration lots,
 * two-phase commit hold/capture operations, and member balances for Loyalty Management System (LMS).
 * 
 * @author Antigravity AI / Telman Şahbazoğlu
 */
@MyServices
@ApplicationScoped
public class LmsLedgerService extends CommonSrv {

    private static final long serialVersionUID = 1L;
    private static final String LMS_DB = "lmsdb";
    private static final String COLLECTION_LEDGER = "transaction_ledger";
    private static final String COLLECTION_MEMBER = "member_profile";
    private static final String COLLECTION_LOTS = "point_lots";

    @Inject
    private LmsRuleEngine lmsRuleEngine;

    @Inject
    private LmsIdempotencyService lmsIdempotencyService;

    @Inject
    private tr.org.tspb.service.lms.LmsAntiFraudService antiFraudService;

    @Inject
    private tr.org.tspb.service.lms.LmsOutboxService outboxService;

    /**
     * Accrues loyalty points for a member based on transaction spend amount and dynamic earn rules.
     * Creates an immutable FIFO Point Lot with expiration timestamp.
     */
    public Document accruePoints(String memberCode, double amount, String orderRef, String programCode, String category) {
        return accruePoints(memberCode, amount, orderRef, programCode, category, null);
    }

    public Document accruePoints(String memberCode, double amount, String orderRef, String programCode, String category, String idempotencyKey) {
        if (idempotencyKey != null) {
            Document cached = lmsIdempotencyService.getCachedResponse(idempotencyKey);
            if (cached != null) {
                logger.info("LMS Accrual Idempotent Hit for Key: [{}]", idempotencyKey);
                return cached;
            }
        }

        if (memberCode == null || memberCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Member code cannot be null or empty");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Transaction amount must be positive");
        }

        // Real-Time Fraud & Anomaly Check
        boolean clear = antiFraudService.evaluateTransactionRisk(memberCode, amount, orderRef);
        if (!clear) {
            throw new IllegalStateException("Transaction blocked by LMS Anti-Fraud Risk Engine for member: " + memberCode);
        }

        Document member = mongoDbUtil.findOne(LMS_DB, COLLECTION_MEMBER, Filters.eq("memberCode", memberCode));
        if (member == null) {
            member = new Document("memberCode", memberCode)
                    .append("fullName", "Member " + memberCode)
                    .append("currentTier", "BRONZE")
                    .append("pointsBalance", 0.0)
                    .append("holdBalance", 0.0)
                    .append("createdDate", new Date());
            mongoDbUtil.insertOne(LMS_DB, COLLECTION_MEMBER, member);
        }

        String currentTier = member.getString("currentTier");
        if (currentTier == null) {
            currentTier = "BRONZE";
        }
        double multiplier = lmsRuleEngine.evaluateEarnMultiplier(currentTier, category, amount);
        double earnedPoints = amount * multiplier;

        Double existingBalance = member.getDouble("pointsBalance");
        if (existingBalance == null) {
            existingBalance = 0.0;
        }
        double newBalance = existingBalance + earnedPoints;

        String txId = "TX-EARN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Date now = new Date();

        // Standard point TTL is 365 days unless overridden by program rules
        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        cal.add(Calendar.DAY_OF_YEAR, 365);
        Date expirationDate = cal.getTime();

        String lotId = "LOT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        PointLot lot = new PointLot(lotId, earnedPoints, earnedPoints, now, expirationDate, orderRef);
        Document lotDoc = lot.toBsonDocument().append("memberCode", memberCode);
        mongoDbUtil.insertOne(LMS_DB, COLLECTION_LOTS, lotDoc);

        Document ledgerEntry = new Document("transactionId", txId)
                .append("memberCode", memberCode)
                .append("type", "EARN")
                .append("points", earnedPoints)
                .append("baseAmount", amount)
                .append("multiplier", multiplier)
                .append("programCode", programCode)
                .append("referenceOrderId", orderRef)
                .append("pointLotId", lotId)
                .append("expirationDate", expirationDate)
                .append("createdDate", now);

        mongoDbUtil.insertOne(LMS_DB, COLLECTION_LEDGER, ledgerEntry);

        String newTier = lmsRuleEngine.evaluateTierProgression(newBalance);
        Document updateMember = new Document("$set", new Document("pointsBalance", newBalance).append("currentTier", newTier));
        mongoDbUtil.updateOne(LMS_DB, COLLECTION_MEMBER, Filters.eq("memberCode", memberCode), updateMember);

        logger.info("LMS Accrual Success: Member [{}] earned [{}] points (Lot: [{}], New Balance: [{}], Tier: [{}])",
                memberCode, earnedPoints, lotId, newBalance, newTier);

        if (idempotencyKey != null) {
            lmsIdempotencyService.storeIdempotentResult(idempotencyKey, "/api/v1/lms/transactions/accrue", ledgerEntry);
        }

        return ledgerEntry;
    }

    /**
     * Redeems loyalty points for a member using FIFO (First-In, First-Out) expiration lot consumption.
     */
    public Document redeemPoints(String memberCode, double pointsToBurn, String rewardRef) {
        return redeemPoints(memberCode, pointsToBurn, rewardRef, null);
    }

    public Document redeemPoints(String memberCode, double pointsToBurn, String rewardRef, String idempotencyKey) {
        if (idempotencyKey != null) {
            Document cached = lmsIdempotencyService.getCachedResponse(idempotencyKey);
            if (cached != null) {
                return cached;
            }
        }

        if (memberCode == null || pointsToBurn <= 0) {
            throw new IllegalArgumentException("Invalid member code or redemption points amount");
        }

        Document member = mongoDbUtil.findOne(LMS_DB, COLLECTION_MEMBER, Filters.eq("memberCode", memberCode));
        if (member == null) {
            throw new IllegalStateException("Member profile not found for code: " + memberCode);
        }

        Double currentBalance = member.getDouble("pointsBalance");
        Double holdBalance = member.getDouble("holdBalance");
        if (holdBalance == null) holdBalance = 0.0;
        double availableBalance = (currentBalance != null ? currentBalance : 0.0) - holdBalance;

        if (availableBalance < pointsToBurn) {
            throw new IllegalStateException("Insufficient available balance. Available: " + availableBalance + ", Required: " + pointsToBurn);
        }

        // FIFO Point Consumption over available active lots
        consumePointsFifo(memberCode, pointsToBurn);

        double newBalance = currentBalance - pointsToBurn;
        String txId = "TX-BURN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Date now = new Date();

        Document ledgerEntry = new Document("transactionId", txId)
                .append("memberCode", memberCode)
                .append("type", "BURN")
                .append("points", -pointsToBurn)
                .append("referenceRewardId", rewardRef)
                .append("createdDate", now);

        mongoDbUtil.insertOne(LMS_DB, COLLECTION_LEDGER, ledgerEntry);

        Document updateMember = new Document("$set", new Document("pointsBalance", newBalance));
        mongoDbUtil.updateOne(LMS_DB, COLLECTION_MEMBER, Filters.eq("memberCode", memberCode), updateMember);

        logger.info("LMS Redemption Success: Member [{}] redeemed [{}] points for reward [{}]", memberCode, pointsToBurn, rewardRef);

        if (idempotencyKey != null) {
            lmsIdempotencyService.storeIdempotentResult(idempotencyKey, "/api/v1/lms/transactions/redeem", ledgerEntry);
        }

        return ledgerEntry;
    }

    /**
     * Phase 1 of 2-Phase Commit: Reserves/Holds points during multi-step checkout.
     */
    public Document holdPoints(String memberCode, double pointsToHold, String orderRef) {
        if (memberCode == null || pointsToHold <= 0) {
            throw new IllegalArgumentException("Invalid member code or points to hold");
        }
        Document member = mongoDbUtil.findOne(LMS_DB, COLLECTION_MEMBER, Filters.eq("memberCode", memberCode));
        if (member == null) {
            throw new IllegalStateException("Member profile not found: " + memberCode);
        }

        Double balance = member.getDouble("pointsBalance");
        Double currentHold = member.getDouble("holdBalance");
        if (balance == null) balance = 0.0;
        if (currentHold == null) currentHold = 0.0;

        double available = balance - currentHold;
        if (available < pointsToHold) {
            throw new IllegalStateException("Insufficient available balance for hold. Available: " + available + ", Requested Hold: " + pointsToHold);
        }

        String holdTxId = "TX-HOLD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Date now = new Date();

        Document holdEntry = new Document("transactionId", holdTxId)
                .append("memberCode", memberCode)
                .append("type", "HOLD")
                .append("points", pointsToHold)
                .append("status", "ACTIVE")
                .append("referenceOrderId", orderRef)
                .append("createdDate", now);

        mongoDbUtil.insertOne(LMS_DB, COLLECTION_LEDGER, holdEntry);

        double newHoldBalance = currentHold + pointsToHold;
        mongoDbUtil.updateOne(LMS_DB, COLLECTION_MEMBER, Filters.eq("memberCode", memberCode),
                new Document("$set", new Document("holdBalance", newHoldBalance)));

        logger.info("LMS Point Hold Success: Member [{}] held [{}] points (Hold Tx: [{}])", memberCode, pointsToHold, holdTxId);
        return holdEntry;
    }

    /**
     * Phase 2 of 2-Phase Commit: Captures a previously held point reservation.
     */
    public Document captureHold(String memberCode, String holdTxId) {
        Document holdEntry = mongoDbUtil.findOne(LMS_DB, COLLECTION_LEDGER, Filters.and(
                Filters.eq("transactionId", holdTxId),
                Filters.eq("type", "HOLD"),
                Filters.eq("status", "ACTIVE")
        ));

        if (holdEntry == null) {
            throw new IllegalStateException("Active hold transaction not found for ID: " + holdTxId);
        }

        double pointsToBurn = holdEntry.getDouble("points");
        String orderRef = holdEntry.getString("referenceOrderId");

        // Release hold balance
        Document member = mongoDbUtil.findOne(LMS_DB, COLLECTION_MEMBER, Filters.eq("memberCode", memberCode));
        Double currentHold = member.getDouble("holdBalance");
        if (currentHold == null) currentHold = 0.0;
        double newHoldBalance = Math.max(0.0, currentHold - pointsToBurn);
        mongoDbUtil.updateOne(LMS_DB, COLLECTION_MEMBER, Filters.eq("memberCode", memberCode),
                new Document("$set", new Document("holdBalance", newHoldBalance)));

        // Update hold status to CAPTURED
        mongoDbUtil.updateOne(LMS_DB, COLLECTION_LEDGER, Filters.eq("transactionId", holdTxId),
                new Document("$set", new Document("status", "CAPTURED")));

        // Execute actual redemption
        return redeemPoints(memberCode, pointsToBurn, orderRef);
    }

    /**
     * Releases/cancels an active point hold reservation.
     */
    public void releaseHold(String memberCode, String holdTxId) {
        Document holdEntry = mongoDbUtil.findOne(LMS_DB, COLLECTION_LEDGER, Filters.and(
                Filters.eq("transactionId", holdTxId),
                Filters.eq("type", "HOLD"),
                Filters.eq("status", "ACTIVE")
        ));

        if (holdEntry != null) {
            double heldPoints = holdEntry.getDouble("points");
            Document member = mongoDbUtil.findOne(LMS_DB, COLLECTION_MEMBER, Filters.eq("memberCode", memberCode));
            Double currentHold = member.getDouble("holdBalance");
            if (currentHold == null) currentHold = 0.0;
            double newHoldBalance = Math.max(0.0, currentHold - heldPoints);

            mongoDbUtil.updateOne(LMS_DB, COLLECTION_MEMBER, Filters.eq("memberCode", memberCode),
                    new Document("$set", new Document("holdBalance", newHoldBalance)));

            mongoDbUtil.updateOne(LMS_DB, COLLECTION_LEDGER, Filters.eq("transactionId", holdTxId),
                    new Document("$set", new Document("status", "RELEASED")));

            logger.info("LMS Point Hold Released: Member [{}] released [{}] points (Hold Tx: [{}])", memberCode, heldPoints, holdTxId);
        }
    }

    /**
     * Consumes points from active point lots using FIFO ordering based on expiration date.
     */
    private void consumePointsFifo(String memberCode, double pointsToConsume) {
        double remainingToConsume = pointsToConsume;
        List<Document> activeLots = mongoDbUtil.find(LMS_DB, COLLECTION_LOTS,
                Filters.and(Filters.eq("memberCode", memberCode), Filters.gt("remainingPoints", 0.0)));

        for (Document lotDoc : activeLots) {
            if (remainingToConsume <= 0) break;
            double lotRemaining = lotDoc.getDouble("remainingPoints");
            String lotId = lotDoc.getString("lotId");

            if (lotRemaining <= remainingToConsume) {
                remainingToConsume -= lotRemaining;
                mongoDbUtil.updateOne(LMS_DB, COLLECTION_LOTS, Filters.eq("lotId", lotId),
                        new Document("$set", new Document("remainingPoints", 0.0)));
            } else {
                double newLotBalance = lotRemaining - remainingToConsume;
                remainingToConsume = 0.0;
                mongoDbUtil.updateOne(LMS_DB, COLLECTION_LOTS, Filters.eq("lotId", lotId),
                        new Document("$set", new Document("remainingPoints", newLotBalance)));
            }
        }
    }

    /**
     * Fetches current member wallet balance, active hold balance, and tier details.
     */
    public Document getMemberWallet(String memberCode) {
        Document member = mongoDbUtil.findOne(LMS_DB, COLLECTION_MEMBER, Filters.eq("memberCode", memberCode));
        if (member != null) {
            Double balance = member.getDouble("pointsBalance");
            Double hold = member.getDouble("holdBalance");
            if (balance == null) balance = 0.0;
            if (hold == null) hold = 0.0;
            member.append("availableBalance", balance - hold);
        }
        return member;
    }

    /**
     * Reverses points earned or redeemed for a given transaction (e.g. product refund / order cancellation).
     * Automatically adjusts member point balance and logs an immutable REVERSAL transaction entry.
     */
    public Document reverseTransactionPoints(String originalTxId, String reason) {
        if (originalTxId == null || originalTxId.trim().isEmpty()) {
            throw new IllegalArgumentException("Original Transaction ID cannot be empty");
        }

        Document origTx = mongoDbUtil.findOne(LMS_DB, COLLECTION_LEDGER, Filters.eq("transactionId", originalTxId));
        if (origTx == null) {
            throw new IllegalArgumentException("Transaction not found for ID: " + originalTxId);
        }

        String memberCode = origTx.getString("memberCode");
        Double points = origTx.getDouble("points");
        if (points == null) points = 0.0;

        Document member = mongoDbUtil.findOne(LMS_DB, COLLECTION_MEMBER, Filters.eq("memberCode", memberCode));
        if (member == null) {
            throw new IllegalStateException("Member not found for transaction: " + memberCode);
        }

        double currentBalance = member.getDouble("pointsBalance") != null ? member.getDouble("pointsBalance") : 0.0;
        double reversalPoints = -points;
        double newBalance = currentBalance + reversalPoints;

        if ("EARN".equalsIgnoreCase(origTx.getString("type")) && points > 0) {
            consumePointsFifo(memberCode, points);
        }

        mongoDbUtil.updateOne(LMS_DB, COLLECTION_MEMBER, Filters.eq("memberCode", memberCode),
                new Document("$set", new Document("pointsBalance", newBalance)));

        String reversalTxId = "REV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Document reversalTxDoc = new Document("transactionId", reversalTxId)
                .append("forms", "transaction_ledger")
                .append("memberCode", memberCode)
                .append("type", "REVERSAL")
                .append("points", reversalPoints)
                .append("referenceOrderId", origTx.getString("referenceOrderId"))
                .append("originalTxId", originalTxId)
                .append("reason", reason != null ? reason : "PRODUCT_RETURN")
                .append("balanceAfter", newBalance)
                .append("createdDate", new Date());

        mongoDbUtil.insertOne(LMS_DB, COLLECTION_LEDGER, reversalTxDoc);

        logger.info("LMS Refund Point Reversal Success: Original Tx [{}] Member [{}] Reversal Points [{}] New Balance [{}]",
                originalTxId, memberCode, reversalPoints, newBalance);

        outboxService.publishEvent("POINT_REVERSAL", reversalTxDoc);

        return reversalTxDoc;
    }
}
