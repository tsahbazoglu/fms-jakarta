package tr.org.tspb.service.lms;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import org.bson.Document;
import com.mongodb.client.model.Filters;

import tr.org.tspb.service.CommonSrv;
import tr.org.tspb.service.lms.util.LmsPiiAnonymizer;
import tr.org.tspb.util.stereotype.MyServices;

/**
 * Enterprise Integration Gateway Service for:
 * 1. 1C:Enterprise (1С:Предприятие 8.3 / Розница) POS payload formatting
 * 2. Viber Business / SMS notification event message builder
 * 3. IFRS 15 / IAS 18 Unearned Point Balance Sheet Liability reporting
 */
@MyServices
@ApplicationScoped
public class LmsIntegrationService extends CommonSrv {

    private static final long serialVersionUID = 1L;
    private static final String LMS_DB = "lmsdb";
    private static final String COLLECTION_MEMBER = "member_profile";
    private static final String COLLECTION_LOTS = "point_lots";

    /**
     * Formats LMS cart calculation results into 1C:Enterprise (1С:Предприятие 8.3) POS JSON structure.
     */
    public Document build1cPosPayload(Document cartResult, String storeId) {
        if (cartResult == null) return new Document();

        List<Document> lineItems = cartResult.getList("lineItems", Document.class);
        List<Document> fiscalItems = new ArrayList<>();
        if (lineItems != null) {
            for (Document item : lineItems) {
                fiscalItems.add(new Document()
                        .append("ProductCode", item.getString("sku"))
                        .append("ProductName", item.getString("name"))
                        .append("Quantity", item.getInteger("quantity"))
                        .append("UnitPrice", item.getDouble("unitPrice"))
                        .append("DiscountAmount", item.getDouble("allocatedDiscount"))
                        .append("NetLineTotal", item.getDouble("netLineTotal")));
            }
        }

        return new Document("Header", new Document("DocumentType", "1C_POS_CHECKOUT")
                .append("StoreCode", storeId)
                .append("Timestamp", new Date())
                .append("SystemSource", "FMS_JAKARTA_LMS"))
                .append("MemberDetails", new Document("MemberCode", cartResult.getString("memberCode"))
                        .append("CurrentTier", cartResult.getString("currentTier")))
                .append("FinancialSummary", new Document("GrossTotal", cartResult.getDouble("totalCartAmount"))
                        .append("TotalDiscount", cartResult.getDouble("totalDiscountAmount"))
                        .append("NetTotal", cartResult.getDouble("netAmount"))
                        .append("EarnedPoints", cartResult.getDouble("projectedEarnedPoints")))
                .append("FiscalLineItems", fiscalItems);
    }

    /**
     * Builds Viber Business / SMS gateway event notification payload.
     */
    public Document buildViberNotificationPayload(String phone, String memberName, double pointsEarned, double totalBalance) {
        String maskedPhone = LmsPiiAnonymizer.maskPhone(phone);
        String text = String.format("Уважаемый(ая) %s! Вам начислено %.2f бонусов. Ваш текущий баланс: %.2f баллов.",
                memberName != null ? memberName : "Клиент", pointsEarned, totalBalance);

        return new Document("receiver", phone)
                .append("maskedReceiver", maskedPhone)
                .append("min_api_version", 1)
                .append("sender", new Document("name", "LMS Bonus"))
                .append("type", "text")
                .append("text", text)
                .append("createdDate", new Date());
    }

    /**
     * Computes total unearned loyalty point financial liability across all active accounts
     * for IFRS 15 / IAS 18 balance sheet accounting compliance.
     */
    public Document generateIfrs15LiabilityReport() {
        List<Document> members = mongoDbUtil.find(LMS_DB, COLLECTION_MEMBER, Filters.exists("pointsBalance"));
        double totalPointLiability = 0.0;
        int activeAccounts = 0;

        if (members != null) {
            for (Document m : members) {
                Double bal = m.getDouble("pointsBalance");
                if (bal != null && bal > 0) {
                    totalPointLiability += bal;
                    activeAccounts++;
                }
            }
        }

        // Standard point monetary valuation rate (e.g. 1 point = 0.10 BYN/TL liability value)
        double valuationRate = 0.10;
        double financialCurrencyLiability = totalPointLiability * valuationRate;

        return new Document("reportName", "IFRS15_UNEARNED_POINT_LIABILITY")
                .append("activeMemberAccounts", activeAccounts)
                .append("totalPointsOutstanding", totalPointLiability)
                .append("pointValuationRate", valuationRate)
                .append("totalFinancialLiabilityAmount", financialCurrencyLiability)
                .append("generatedDate", new Date());
    }
}
