package com.prashant.milkdairy.Model;

public class BillingReportModel {
    public String farmerCode;
    public String farmerName;
    public double milkAmount;
    public double bonus;
    public double deduction;
    public double netPayable;
    public String status;

    public BillingReportModel(String farmerCode, String farmerName,
                              double milkAmount, double bonus, double deduction,
                              double netPayable, String status) {
        this.farmerCode  = farmerCode;
        this.farmerName  = farmerName;
        this.milkAmount  = milkAmount;
        this.bonus       = bonus;
        this.deduction   = deduction;
        this.netPayable  = netPayable;
        this.status      = status;
    }
}