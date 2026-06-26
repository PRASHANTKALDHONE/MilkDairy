package com.prashant.milkdairy.Model;

public class YieldReportModel {
    public String farmerName;
    public double cowLiters;
    public double buffaloLiters;
    public double mixLiters;
    public double cowAmount;
    public double buffaloAmount;
    public double totalAmount;

    public YieldReportModel(String farmerName,
                            double cowLiters, double buffaloLiters, double mixLiters,
                            double cowAmount, double buffaloAmount, double totalAmount) {
        this.farmerName     = farmerName;
        this.cowLiters      = cowLiters;
        this.buffaloLiters  = buffaloLiters;
        this.mixLiters      = mixLiters;
        this.cowAmount      = cowAmount;
        this.buffaloAmount  = buffaloAmount;
        this.totalAmount    = totalAmount;
    }
}