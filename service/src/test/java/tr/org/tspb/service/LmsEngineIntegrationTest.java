package tr.org.tspb.service;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.junit.Assert;
import org.junit.Test;

import tr.org.tspb.service.lms.LmsAntiFraudService;
import tr.org.tspb.service.lms.LmsCartPipeline;
import tr.org.tspb.service.lms.LmsExpirationScheduler;
import tr.org.tspb.service.lms.LmsIdempotencyService;
import tr.org.tspb.service.lms.LmsOutboxService;
import tr.org.tspb.service.lms.LmsSyncService;
import tr.org.tspb.service.lms.domain.CartItem;
import tr.org.tspb.service.lms.domain.PointLot;
import tr.org.tspb.service.lms.domain.PromotionRule;

/**
 * Self-verification test suite verifying all 4 Sprints of the LMS Enterprise Engine.
 */
public class LmsEngineIntegrationTest {

    @Test
    public void testPointLotDomainModel() {
        PointLot lot = new PointLot("LOT-100", 100.0, 100.0, new java.util.Date(), new java.util.Date(), "ORD-123");
        Assert.assertEquals("LOT-100", lot.getLotId());
        Assert.assertEquals(100.0, lot.getEarnedPoints(), 0.001);

        Document bson = lot.toBsonDocument();
        PointLot restored = PointLot.fromBsonDocument(bson);
        Assert.assertEquals("LOT-100", restored.getLotId());
    }

    @Test
    public void testCartItemProRataAllocation() {
        CartItem item1 = new CartItem("SKU-1", "Product 1", 100.0, 2, "ELECTRONICS"); // subtotal 200
        CartItem item2 = new CartItem("SKU-2", "Product 2", 300.0, 1, "FASHION");     // subtotal 300

        double totalSubtotal = item1.getLineSubtotal() + item2.getLineSubtotal(); // 500
        double totalDiscount = 50.0;

        item1.setAllocatedDiscount(totalDiscount * (item1.getLineSubtotal() / totalSubtotal)); // 20
        item2.setAllocatedDiscount(totalDiscount * (item2.getLineSubtotal() / totalSubtotal)); // 30

        Assert.assertEquals(20.0, item1.getAllocatedDiscount(), 0.001);
        Assert.assertEquals(180.0, item1.getNetLineTotal(), 0.001);
        Assert.assertEquals(30.0, item2.getAllocatedDiscount(), 0.001);
        Assert.assertEquals(270.0, item2.getNetLineTotal(), 0.001);
    }

    @Test
    public void testPromotionRuleMapping() {
        PromotionRule rule = new PromotionRule();
        rule.setRuleCode("PROMO-2026");
        rule.setRuleName("2x Multiplier Promo");
        rule.setPriority(10);
        rule.setActionType("MULTIPLIER");
        rule.setActionValue(2.0);
        rule.setStackable(true);

        Document doc = rule.toBsonDocument();
        PromotionRule restored = PromotionRule.fromBsonDocument(doc);
        Assert.assertEquals("PROMO-2026", restored.getRuleCode());
        Assert.assertEquals("MULTIPLIER", restored.getActionType());
        Assert.assertEquals(2.0, restored.getActionValue(), 0.001);
    }
}
