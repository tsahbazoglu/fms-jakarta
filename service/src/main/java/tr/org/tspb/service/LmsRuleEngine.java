package tr.org.tspb.service;

import jakarta.enterprise.context.ApplicationScoped;
import tr.org.tspb.util.stereotype.MyServices;

/**
 * Dynamic Rule and Multiplier Evaluator Engine for LMS.
 * 
 * @author Antigravity AI / Telman Şahbazoğlu
 */
@MyServices
@ApplicationScoped
public class LmsRuleEngine extends CommonSrv {

    private static final long serialVersionUID = 1L;

    /**
     * Evaluates dynamic points multiplier based on member tier, product category, and spend amount.
     */
    public double evaluateEarnMultiplier(String currentTier, String category, double amount) {
        double multiplier = 1.0; // Base rate: 1 point per 1 currency unit

        if ("SILVER".equalsIgnoreCase(currentTier)) {
            multiplier = 1.25;
        } else if ("GOLD".equalsIgnoreCase(currentTier)) {
            multiplier = 1.50;
        } else if ("PLATINUM".equalsIgnoreCase(currentTier)) {
            multiplier = 2.00;
        }

        // Special category bonus (e.g. ELECTRONICS or TRAVEL)
        if ("ELECTRONICS".equalsIgnoreCase(category)) {
            multiplier += 0.50;
        } else if ("TRAVEL".equalsIgnoreCase(category)) {
            multiplier += 1.00;
        }

        // High spender threshold bonus (> 1000 TL)
        if (amount >= 1000.0) {
            multiplier += 0.25;
        }

        return multiplier;
    }

    /**
     * Evaluates tier status based on total point balance thresholds.
     */
    public String evaluateTierProgression(double totalPoints) {
        if (totalPoints >= 5000.0) {
            return "PLATINUM";
        } else if (totalPoints >= 2000.0) {
            return "GOLD";
        } else if (totalPoints >= 500.0) {
            return "SILVER";
        }
        return "BRONZE";
    }
}
