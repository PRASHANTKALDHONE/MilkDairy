package com.prashant.milkdairy.Model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class InventoryModel {

    private String itemId;
    private String itemCode;
    private String itemName;
    private String category;
    private String unit;

    private double currentStock;
    private double minimumStock;
    private double purchaseRate;
    private double sellingRate;
    private double stockValue;

    private boolean active = true;
    private long updatedAtMillis;

    public InventoryModel() {}

    public static InventoryModel fromDoc(DocumentSnapshot doc) {
        InventoryModel model = new InventoryModel();

        model.itemId = doc.getId();
        model.itemCode = safe(doc.getString("itemCode"));
        model.itemName = safe(doc.getString("itemName"));
        model.category = safe(doc.getString("category"));
        model.unit = safe(doc.getString("unit"));

        model.currentStock = getDouble(doc, "currentStock");
        model.minimumStock = getDouble(doc, "minimumStock");
        model.purchaseRate = getDouble(doc, "purchaseRate");
        model.sellingRate = getDouble(doc, "sellingRate");
        model.stockValue = getDouble(doc, "stockValue");

        Boolean isActive = doc.getBoolean("isActive");
        model.active = isActive == null || isActive;

        Timestamp updatedAt = doc.getTimestamp("updatedAt");
        model.updatedAtMillis = updatedAt == null ? 0 : updatedAt.toDate().getTime();

        return model;
    }

    private static double getDouble(DocumentSnapshot doc, String key) {
        Double value = doc.getDouble(key);
        return value == null ? 0 : value;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private String money(double value) {
        return "₹" + String.format(Locale.US, "%.2f", value);
    }

    private String number(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    public String getItemId() {
        return itemId;
    }

    public String getCode() {
        return itemCode;
    }

    public String getItemCode() {
        return itemCode;
    }

    public String getItemName() {
        return itemName;
    }

    public String getCategory() {
        return category;
    }

    public String getUnit() {
        return unit;
    }

    public double getCurrentStockValue() {
        return currentStock;
    }

    public double getMinimumStockValue() {
        return minimumStock;
    }

    public double getPurchaseRateValue() {
        return purchaseRate;
    }

    public double getSellingRateValue() {
        return sellingRate;
    }

    public double getStockValueAmount() {
        return currentStock * purchaseRate;
    }

    public boolean isActive() {
        return active;
    }

    public String getStock() {
        return number(currentStock) + " " + unit;
    }

    public String getMinStock() {
        return number(minimumStock);
    }

    public String getPurchaseRate() {
        return money(purchaseRate);
    }

    public String getSellingRate() {
        return money(sellingRate);
    }

    public String getStockValue() {
        return money(getStockValueAmount());
    }

    public String getLastUpdated() {
        if (updatedAtMillis <= 0) return "-";

        return new SimpleDateFormat("dd MMM yyyy", Locale.US)
                .format(new java.util.Date(updatedAtMillis));
    }
}
