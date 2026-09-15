package tr.org.tspb.service.lms.domain;

import java.io.Serializable;
import java.util.Date;
import org.bson.Document;

/**
 * Represents an immutable point lot/bucket with expiration timestamp for FIFO point redemption.
 */
public class PointLot implements Serializable {

    private static final long serialVersionUID = 1L;

    private String lotId;
    private double earnedPoints;
    private double remainingPoints;
    private Date createdDate;
    private Date expirationDate;
    private String referenceOrderId;

    public PointLot() {
    }

    public PointLot(String lotId, double earnedPoints, double remainingPoints, Date createdDate, Date expirationDate, String referenceOrderId) {
        this.lotId = lotId;
        this.earnedPoints = earnedPoints;
        this.remainingPoints = remainingPoints;
        this.createdDate = createdDate;
        this.expirationDate = expirationDate;
        this.referenceOrderId = referenceOrderId;
    }

    public String getLotId() {
        return lotId;
    }

    public void setLotId(String lotId) {
        this.lotId = lotId;
    }

    public double getEarnedPoints() {
        return earnedPoints;
    }

    public void setEarnedPoints(double earnedPoints) {
        this.earnedPoints = earnedPoints;
    }

    public double getRemainingPoints() {
        return remainingPoints;
    }

    public void setRemainingPoints(double remainingPoints) {
        this.remainingPoints = remainingPoints;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }

    public String getReferenceOrderId() {
        return referenceOrderId;
    }

    public void setReferenceOrderId(String referenceOrderId) {
        this.referenceOrderId = referenceOrderId;
    }

    public Document toBsonDocument() {
        return new Document("lotId", lotId)
                .append("earnedPoints", earnedPoints)
                .append("remainingPoints", remainingPoints)
                .append("createdDate", createdDate)
                .append("expirationDate", expirationDate)
                .append("referenceOrderId", referenceOrderId);
    }

    public static PointLot fromBsonDocument(Document doc) {
        if (doc == null) {
            return null;
        }
        PointLot lot = new PointLot();
        lot.setLotId(doc.getString("lotId"));
        Double earned = doc.getDouble("earnedPoints");
        lot.setEarnedPoints(earned != null ? earned : 0.0);
        Double remaining = doc.getDouble("remainingPoints");
        lot.setRemainingPoints(remaining != null ? remaining : 0.0);
        lot.setCreatedDate(doc.getDate("createdDate"));
        lot.setExpirationDate(doc.getDate("expirationDate"));
        lot.setReferenceOrderId(doc.getString("referenceOrderId"));
        return lot;
    }
}
