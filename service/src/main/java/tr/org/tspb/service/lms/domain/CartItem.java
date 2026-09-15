package tr.org.tspb.service.lms.domain;

import java.io.Serializable;
import org.bson.Document;

/**
 * Represents an individual line item inside a POS / e-commerce shopping cart.
 */
public class CartItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private String sku;
    private String name;
    private double unitPrice;
    private int quantity;
    private String category;
    private double lineSubtotal;
    private double allocatedDiscount;
    private double netLineTotal;
    private double earnedPoints;

    public CartItem() {
    }

    public CartItem(String sku, String name, double unitPrice, int quantity, String category) {
        this.sku = sku;
        this.name = name;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.category = category;
        this.lineSubtotal = unitPrice * quantity;
        this.allocatedDiscount = 0.0;
        this.netLineTotal = this.lineSubtotal;
        this.earnedPoints = 0.0;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
        this.lineSubtotal = this.unitPrice * quantity;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getLineSubtotal() {
        return lineSubtotal;
    }

    public void setLineSubtotal(double lineSubtotal) {
        this.lineSubtotal = lineSubtotal;
    }

    public double getAllocatedDiscount() {
        return allocatedDiscount;
    }

    public void setAllocatedDiscount(double allocatedDiscount) {
        this.allocatedDiscount = allocatedDiscount;
        this.netLineTotal = Math.max(0.0, this.lineSubtotal - allocatedDiscount);
    }

    public double getNetLineTotal() {
        return netLineTotal;
    }

    public void setNetLineTotal(double netLineTotal) {
        this.netLineTotal = netLineTotal;
    }

    public double getEarnedPoints() {
        return earnedPoints;
    }

    public void setEarnedPoints(double earnedPoints) {
        this.earnedPoints = earnedPoints;
    }

    public Document toBsonDocument() {
        return new Document("sku", sku)
                .append("name", name)
                .append("unitPrice", unitPrice)
                .append("quantity", quantity)
                .append("category", category)
                .append("lineSubtotal", lineSubtotal)
                .append("allocatedDiscount", allocatedDiscount)
                .append("netLineTotal", netLineTotal)
                .append("earnedPoints", earnedPoints);
    }
}
