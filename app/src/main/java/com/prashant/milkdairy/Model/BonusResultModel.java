package com.prashant.milkdairy.Model;

public class BonusResultModel {

    private String farmerId;
    private String farmerCode;
    private String farmerName;
    private double totalLiters;
    private double milkAmount;
    private double bonusAmount;

    public BonusResultModel(String farmerId, String farmerCode, String farmerName,
                            double totalLiters, double milkAmount, double bonusAmount) {
        this.farmerId = farmerId;
        this.farmerCode = farmerCode;
        this.farmerName = farmerName;
        this.totalLiters = totalLiters;
        this.milkAmount = milkAmount;
        this.bonusAmount = bonusAmount;
    }

    public String getFarmerId() { return farmerId; }
    public String getFarmerCode() { return farmerCode; }
    public String getFarmerName() { return farmerName; }
    public double getTotalLiters() { return totalLiters; }
    public double getMilkAmount() { return milkAmount; }
    public double getBonusAmount() { return bonusAmount; }
}
