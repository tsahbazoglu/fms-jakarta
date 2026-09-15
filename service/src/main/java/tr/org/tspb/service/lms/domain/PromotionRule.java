package tr.org.tspb.service.lms.domain;

import java.io.Serializable;
import org.bson.Document;

/**
 * Represents a dynamic promotion rule evaluated by the LMS Rule Engine during cart calculations.
 */
public class PromotionRule implements Serializable {

    private static final long serialVersionUID = 1L;

    private String ruleCode;
    private String ruleName;
    private int priority;
    private String actionType; // MULTIPLIER, DISCOUNT_PCT, DISCOUNT_FIXED, FREE_GIFT
    private double actionValue;
    private double minSubtotal;
    private String requiredTier;
    private String targetCategory;
    private String targetSku;
    private double maxDiscountCap;
    private boolean stackable;
    private boolean active;
    private String expression; // Dynamic condition expression (e.g. cart.totalAmount >= 50.0 && customer.tier == 'GOLD')
    private String stackingMode; // EXCLUSIVE, ADDITIVE, BEST_VALUE
    private double budgetCap; // Maximum allowed campaign discount budget allowance
    private double currentSpentBudget; // Total discount payout accumulated so far
    private String approvalStatus; // DRAFT, PENDING_APPROVAL, APPROVED, REJECTED
    private String approvedBy;

    public PromotionRule() {
        this.active = true;
        this.stackable = true;
        this.stackingMode = "ADDITIVE";
        this.approvalStatus = "APPROVED";
        this.budgetCap = 0.0;
        this.currentSpentBudget = 0.0;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public double getActionValue() {
        return actionValue;
    }

    public void setActionValue(double actionValue) {
        this.actionValue = actionValue;
    }

    public double getMinSubtotal() {
        return minSubtotal;
    }

    public void setMinSubtotal(double minSubtotal) {
        this.minSubtotal = minSubtotal;
    }

    public String getRequiredTier() {
        return requiredTier;
    }

    public void setRequiredTier(String requiredTier) {
        this.requiredTier = requiredTier;
    }

    public String getTargetCategory() {
        return targetCategory;
    }

    public void setTargetCategory(String targetCategory) {
        this.targetCategory = targetCategory;
    }

    public String getTargetSku() {
        return targetSku;
    }

    public void setTargetSku(String targetSku) {
        this.targetSku = targetSku;
    }

    public double getMaxDiscountCap() {
        return maxDiscountCap;
    }

    public void setMaxDiscountCap(double maxDiscountCap) {
        this.maxDiscountCap = maxDiscountCap;
    }

    public boolean isStackable() {
        return stackable;
    }

    public void setStackable(boolean stackable) {
        this.stackable = stackable;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public String getStackingMode() {
        return stackingMode != null ? stackingMode : "ADDITIVE";
    }

    public void setStackingMode(String stackingMode) {
        this.stackingMode = stackingMode;
    }

    public double getBudgetCap() {
        return budgetCap;
    }

    public void setBudgetCap(double budgetCap) {
        this.budgetCap = budgetCap;
    }

    public double getCurrentSpentBudget() {
        return currentSpentBudget;
    }

    public void setCurrentSpentBudget(double currentSpentBudget) {
        this.currentSpentBudget = currentSpentBudget;
    }

    public String getApprovalStatus() {
        return approvalStatus != null ? approvalStatus : "APPROVED";
    }

    public void setApprovalStatus(String approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public static PromotionRule fromBsonDocument(Document doc) {
        if (doc == null) return null;
        PromotionRule rule = new PromotionRule();
        rule.setRuleCode(doc.getString("ruleCode"));
        rule.setRuleName(doc.getString("ruleName"));
        Integer prio = doc.getInteger("priority");
        rule.setPriority(prio != null ? prio : 100);
        rule.setActionType(doc.getString("actionType"));
        Double val = doc.getDouble("actionValue");
        rule.setActionValue(val != null ? val : 0.0);
        Double minSub = doc.getDouble("minSubtotal");
        rule.setMinSubtotal(minSub != null ? minSub : 0.0);
        rule.setRequiredTier(doc.getString("requiredTier"));
        rule.setTargetCategory(doc.getString("targetCategory"));
        rule.setTargetSku(doc.getString("targetSku"));
        Double cap = doc.getDouble("maxDiscountCap");
        rule.setMaxDiscountCap(cap != null ? cap : 0.0);
        Boolean stk = doc.getBoolean("stackable");
        rule.setStackable(stk != null ? stk : true);
        Boolean act = doc.getBoolean("active");
        rule.setActive(act != null ? act : true);
        rule.setExpression(doc.getString("expression"));
        String stkMode = doc.getString("stackingMode");
        rule.setStackingMode(stkMode != null ? stkMode : (rule.isStackable() ? "ADDITIVE" : "EXCLUSIVE"));
        Double bCap = doc.getDouble("budgetCap");
        rule.setBudgetCap(bCap != null ? bCap : 0.0);
        Double cSpent = doc.getDouble("currentSpentBudget");
        rule.setCurrentSpentBudget(cSpent != null ? cSpent : 0.0);
        String appStatus = doc.getString("approvalStatus");
        rule.setApprovalStatus(appStatus != null ? appStatus : "APPROVED");
        rule.setApprovedBy(doc.getString("approvedBy"));
        return rule;
    }

    public Document toBsonDocument() {
        return new Document("ruleCode", ruleCode)
                .append("ruleName", ruleName)
                .append("priority", priority)
                .append("actionType", actionType)
                .append("actionValue", actionValue)
                .append("minSubtotal", minSubtotal)
                .append("requiredTier", requiredTier)
                .append("targetCategory", targetCategory)
                .append("targetSku", targetSku)
                .append("maxDiscountCap", maxDiscountCap)
                .append("stackable", stackable)
                .append("active", active)
                .append("expression", expression)
                .append("stackingMode", stackingMode)
                .append("budgetCap", budgetCap)
                .append("currentSpentBudget", currentSpentBudget)
                .append("approvalStatus", approvalStatus)
                .append("approvedBy", approvedBy);
    }
}
