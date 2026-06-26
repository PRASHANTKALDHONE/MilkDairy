package com.prashant.milkdairy.Model;

public class InventoryReportModel {
    public String itemName;
    public String category;
    public double stockIn;
    public double stockOut;
    public double currentStock;
    public String unit;
    public double stockValue;

    public InventoryReportModel(String itemName, String category,
                                double stockIn, double stockOut, double currentStock,
                                String unit, double stockValue) {
        this.itemName     = itemName;
        this.category     = category;
        this.stockIn      = stockIn;
        this.stockOut     = stockOut;
        this.currentStock = currentStock;
        this.unit         = unit;
        this.stockValue   = stockValue;
    }
}