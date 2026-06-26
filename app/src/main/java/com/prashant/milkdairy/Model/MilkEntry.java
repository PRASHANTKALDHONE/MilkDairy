package com.prashant.milkdairy.Model;

public class MilkEntry {

    private String shift;
    private String farmerName;
    private String mobile;
    private double liters;
    private double fat;
    private double snf;
    private double rate;
    private double amount;

    public MilkEntry() {
        // Required for Firestore
    }

    public MilkEntry(String shift,
                     String farmerName,
                     String mobile,
                     double liters,
                     double fat,
                     double snf,
                     double rate,
                     double amount) {

        this.shift = shift;
        this.farmerName = farmerName;
        this.mobile = mobile;
        this.liters = liters;
        this.fat = fat;
        this.snf = snf;
        this.rate = rate;
        this.amount = amount;
    }

    public String getShift() {
        return shift;
    }

    public String getFarmerName() {
        return farmerName;
    }

    public String getMobile() {
        return mobile;
    }

    public double getLiters() {
        return liters;
    }

    public double getFat() {
        return fat;
    }

    public double getSnf() {
        return snf;
    }

    public double getRate() {
        return rate;
    }

    public double getAmount() {
        return amount;
    }
}