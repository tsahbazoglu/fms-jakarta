package tr.org.tspb.service.lms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.Document;
import com.mongodb.client.model.Filters;

import tr.org.tspb.service.CommonSrv;
import tr.org.tspb.service.LmsRuleEngine;
import tr.org.tspb.service.lms.domain.CartItem;
import tr.org.tspb.service.lms.domain.PromotionRule;
import tr.org.tspb.util.stereotype.MyServices;

/**
 * High-Performance Cart Calculation Pipeline evaluating promotion rules, allocating line-item discounts,
 * and projecting points accrual under strict SLA (<50ms).
 */
@MyServices
@ApplicationScoped
public class LmsCartPipeline extends CommonSrv {

    private static final long serialVersionUID = 1L;
    private static final String LMS_DB = "lmsdb";
    private static final String COLLECTION_RULES = "rule_promotions";
    private static final String COLLECTION_MEMBER = "member_profile";
    private static final String COLLECTION_PROGRAM = "loyalty_program";

    @Inject
    private LmsRuleEngine lmsRuleEngine;

    // Compiled L1 Rule Cache (Invalidated on admin save or 60s TTL)
    private static final ConcurrentHashMap<String, List<PromotionRule>> RULE_CACHE = new ConcurrentHashMap<>();
    private static long lastCacheTime = 0L;
    private static final long CACHE_TTL_MS = TimeUnit.SECONDS.toMillis(60);

    /**
     * Executes the cart calculation pipeline for a given member and cart request payload.
     */
    public Document calculateCart(String memberCode, List<CartItem> items, String storeId, List<String> appliedPromocodes) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Cart must contain at least one item");
        }

        // 1. Fetch Member Profile & Tier
        String currentTier = "BRONZE";
        if (memberCode != null && !memberCode.trim().isEmpty()) {
            Document member = mongoDbUtil.findOne(LMS_DB, COLLECTION_MEMBER, Filters.eq("memberCode", memberCode));
            if (member != null && member.getString("currentTier") != null) {
                currentTier = member.getString("currentTier");
            }
        }

        // 2. Compute Initial Cart Subtotal
        double totalCartAmount = 0.0;
        for (CartItem item : items) {
            totalCartAmount += item.getLineSubtotal();
        }

        // 3. Fetch Active Rules (with L1 Caching sorted by Priority DESC)
        List<PromotionRule> activeRules = getActiveRules();

        // 4. Evaluate Applicable Rules with Stacking Matrix (EXCLUSIVE, BEST_VALUE, ADDITIVE)
        double totalDiscount = 0.0;
        double activeMultiplier = 1.0;
        List<String> appliedRuleCodes = new ArrayList<>();
        boolean exclusiveRuleApplied = false;
        int totalCartItemCount = items.size();

        for (PromotionRule rule : activeRules) {
            if (!rule.isActive()) continue;

            // Financial Approval Governance Check
            if (!"APPROVED".equalsIgnoreCase(rule.getApprovalStatus())) {
                continue;
            }

            // Budget Cap Guardrail Check
            if (rule.getBudgetCap() > 0 && rule.getCurrentSpentBudget() >= rule.getBudgetCap()) {
                logger.warn("LMS Rule [{}] budget cap of [{}] exhausted (spent: [{}]) - bypassing rule",
                        rule.getRuleCode(), rule.getBudgetCap(), rule.getCurrentSpentBudget());
                continue;
            }

            // Stacking Check: If EXCLUSIVE rule has already been applied, stop further promotions
            if (exclusiveRuleApplied) break;

            String mode = rule.getStackingMode();
            if ("EXCLUSIVE".equalsIgnoreCase(mode) && !appliedRuleCodes.isEmpty()) {
                // EXCLUSIVE rules only apply if no other rule has been applied yet
                continue;
            }

            // Tier check
            if (rule.getRequiredTier() != null && !"ALL".equalsIgnoreCase(rule.getRequiredTier())) {
                if (!rule.getRequiredTier().equalsIgnoreCase(currentTier)) {
                    continue;
                }
            }

            // Subtotal check
            if (totalCartAmount < rule.getMinSubtotal()) {
                continue;
            }

            // Dynamic Expression Evaluation
            if (!evaluateRuleExpression(rule, currentTier, totalCartAmount, totalCartItemCount)) {
                continue;
            }

            // Rule Execution & Stacking Resolution
            boolean ruleMatched = false;
            String type = rule.getActionType();
            double val = rule.getActionValue();
            double evaluatedDiscount = 0.0;

            if ("MULTIPLIER".equalsIgnoreCase(type)) {
                activeMultiplier = Math.max(activeMultiplier, val);
                ruleMatched = true;
            } else if ("DISCOUNT_PCT".equalsIgnoreCase(type)) {
                evaluatedDiscount = totalCartAmount * (val / 100.0);
                if (rule.getMaxDiscountCap() > 0) {
                    evaluatedDiscount = Math.min(evaluatedDiscount, rule.getMaxDiscountCap());
                }
                ruleMatched = true;
            } else if ("DISCOUNT_FIXED".equalsIgnoreCase(type)) {
                evaluatedDiscount = val;
                ruleMatched = true;
            }

            if (ruleMatched) {
                totalDiscount += evaluatedDiscount;
                appliedRuleCodes.add(rule.getRuleCode() + ": " + rule.getRuleName() + " [" + mode + "]");

                if ("EXCLUSIVE".equalsIgnoreCase(mode) || !rule.isStackable()) {
                    exclusiveRuleApplied = true;
                }
            }
        }

        // Cap total discount to cart subtotal
        totalDiscount = Math.min(totalDiscount, totalCartAmount);
        double netAmount = totalCartAmount - totalDiscount;

        // 5. Pro-Rata Line Item Discount Allocation (Item-Level Distribution)
        for (CartItem item : items) {
            if (totalCartAmount > 0) {
                double itemShareRatio = item.getLineSubtotal() / totalCartAmount;
                double allocatedItemDiscount = totalDiscount * itemShareRatio;
                item.setAllocatedDiscount(allocatedItemDiscount);
            } else {
                item.setAllocatedDiscount(0.0);
            }
        }

        // 6. Projected Points Accrual Calculation
        double baseEarnRate = 0.10; // Default: 1 TL = 0.10 Puan
        Document program = mongoDbUtil.findOne(LMS_DB, COLLECTION_PROGRAM, Filters.eq("active", true));
        if (program != null && program.getDouble("baseEarnRate") != null) {
            baseEarnRate = program.getDouble("baseEarnRate");
        }

        double tierMultiplier = lmsRuleEngine.evaluateEarnMultiplier(currentTier, "GENERAL", netAmount);
        double totalEarnedPoints = netAmount * baseEarnRate * tierMultiplier * activeMultiplier;

        // Pro-rata distribute points to items
        List<Document> itemDocs = new ArrayList<>();
        for (CartItem item : items) {
            if (netAmount > 0) {
                double itemPointShare = totalEarnedPoints * (item.getNetLineTotal() / netAmount);
                item.setEarnedPoints(itemPointShare);
            }
            itemDocs.add(item.toBsonDocument());
        }

        logger.info("LMS Cart Pipeline Success: Member [{}] Subtotal [{}] Discount [{}] Net [{}] Points [{}] Rules [{}]",
                memberCode, totalCartAmount, totalDiscount, netAmount, totalEarnedPoints, appliedRuleCodes.size());

        return new Document("memberCode", memberCode)
                .append("currentTier", currentTier)
                .append("totalCartAmount", totalCartAmount)
                .append("totalDiscountAmount", totalDiscount)
                .append("netAmount", netAmount)
                .append("projectedEarnedPoints", totalEarnedPoints)
                .append("tierMultiplier", tierMultiplier)
                .append("activeMultiplier", activeMultiplier)
                .append("appliedRules", appliedRuleCodes)
                .append("lineItems", itemDocs)
                .append("calculatedDate", new Date());
    }

    /**
     * Evaluates dynamic condition expressions against cart and member context.
     * Supported variables: cart.totalAmount, cart.itemCount, customer.tier
     */
    private boolean evaluateRuleExpression(PromotionRule rule, String currentTier, double totalCartAmount, int itemCount) {
        String expr = rule.getExpression();
        if (expr == null || expr.trim().isEmpty()) {
            return true; // No expression = default true
        }

        try {
            String sanitized = expr.trim()
                    .replace("cart.totalAmount", String.valueOf(totalCartAmount))
                    .replace("cart.itemCount", String.valueOf(itemCount))
                    .replace("customer.tier", "'" + currentTier + "'");

            if (sanitized.contains("&&")) {
                String[] parts = sanitized.split("&&");
                for (String part : parts) {
                    if (!evaluateSingleCondition(part.trim(), currentTier, totalCartAmount, itemCount)) {
                        return false;
                    }
                }
                return true;
            } else if (sanitized.contains("||")) {
                String[] parts = sanitized.split("\\|\\|");
                for (String part : parts) {
                    if (evaluateSingleCondition(part.trim(), currentTier, totalCartAmount, itemCount)) {
                        return true;
                    }
                }
                return false;
            } else {
                return evaluateSingleCondition(sanitized, currentTier, totalCartAmount, itemCount);
            }
        } catch (Exception e) {
            logger.warn("Rule Expression evaluation failed for rule [{}]: {}", rule.getRuleCode(), e.getMessage());
            return true;
        }
    }

    private boolean evaluateSingleCondition(String cond, String currentTier, double totalCartAmount, int itemCount) {
        if (cond.contains(">=")) {
            String[] s = cond.split(">=");
            return parseVal(s[0]) >= parseVal(s[1]);
        } else if (cond.contains("<=")) {
            String[] s = cond.split("<=");
            return parseVal(s[0]) <= parseVal(s[1]);
        } else if (cond.contains(">")) {
            String[] s = cond.split(">");
            return parseVal(s[0]) > parseVal(s[1]);
        } else if (cond.contains("<")) {
            String[] s = cond.split("<");
            return parseVal(s[0]) < parseVal(s[1]);
        } else if (cond.contains("==")) {
            String[] s = cond.split("==");
            String left = s[0].trim().replace("'", "");
            String right = s[1].trim().replace("'", "");
            return left.equalsIgnoreCase(right);
        } else if (cond.contains("!=")) {
            String[] s = cond.split("!=");
            String left = s[0].trim().replace("'", "");
            String right = s[1].trim().replace("'", "");
            return !left.equalsIgnoreCase(right);
        }
        return true;
    }

    private double parseVal(String str) {
        try {
            return Double.parseDouble(str.trim().replace("'", ""));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * Retrieves active rules sorted by priority DESC from Caffeine/L1 cache or MongoDB.
     */
    private List<PromotionRule> getActiveRules() {
        long now = System.currentTimeMillis();
        if (now - lastCacheTime < CACHE_TTL_MS && RULE_CACHE.containsKey("ACTIVE_RULES")) {
            return RULE_CACHE.get("ACTIVE_RULES");
        }

        List<Document> ruleDocs = mongoDbUtil.find(LMS_DB, COLLECTION_RULES, Filters.eq("active", true));
        List<PromotionRule> rules = new ArrayList<>();
        if (ruleDocs != null) {
            for (Document doc : ruleDocs) {
                PromotionRule r = PromotionRule.fromBsonDocument(doc);
                if (r != null) rules.add(r);
            }
        }

        // Sort rules by priority DESC (higher priority evaluated first)
        Collections.sort(rules, (r1, r2) -> Integer.compare(r2.getPriority(), r1.getPriority()));

        RULE_CACHE.put("ACTIVE_RULES", rules);
        lastCacheTime = now;
        return rules;
    }

    /**
     * Clears the L1 rule cache to support real-time rule updates.
     */
    public static void invalidateRuleCache() {
        RULE_CACHE.clear();
        lastCacheTime = 0L;
    }
}
