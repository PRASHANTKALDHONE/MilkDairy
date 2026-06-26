package com.prashant.milkdairy.Model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BillModel {

    private String id, billNo, farmerId, farmerName, farmerCode, mobile;
    private String periodFrom, periodTo, status;

    private double totalLiters, milkAmount, bonusAmount, deductionAmount, netPayable;

    private List<Map<String, Object>> milkSnapshot = new ArrayList<>();
    private List<Map<String, Object>> bonusSnapshot = new ArrayList<>();
    private List<Map<String, Object>> deductionSnapshot = new ArrayList<>();

    public BillModel() {}

    public String getId() { return id; }
    public String getBillNo() { return billNo; }
    public String getFarmerId() { return farmerId; }
    public String getFarmerName() { return farmerName; }
    public String getFarmerCode() { return farmerCode; }
    public String getMobile() { return mobile; }
    public String getPeriodFrom() { return periodFrom; }
    public String getPeriodTo() { return periodTo; }
    public String getStatus() { return status; }

    public double getTotalLitersValue() { return totalLiters; }
    public double getMilkAmountValue() { return milkAmount; }
    public double getBonusAmountValue() { return bonusAmount; }
    public double getDeductionAmountValue() { return deductionAmount; }
    public double getNetPayableValue() { return netPayable; }

    public List<Map<String, Object>> getMilkSnapshot() { return milkSnapshot; }
    public List<Map<String, Object>> getBonusSnapshot() { return bonusSnapshot; }
    public List<Map<String, Object>> getDeductionSnapshot() { return deductionSnapshot; }

    public String getPeriod() { return periodFrom + " - " + periodTo; }
    public String getMilkAmount() { return String.format(Locale.US, "%.2f", milkAmount); }
    public String getBonus() { return String.format(Locale.US, "%.2f", bonusAmount); }
    public String getDeduction() { return String.format(Locale.US, "%.2f", deductionAmount); }
    public String getNetPayable() { return String.format(Locale.US, "%.2f", netPayable); }

    public void setId(String id) { this.id = id; }
    public void setBillNo(String billNo) { this.billNo = billNo; }
    public void setFarmerId(String farmerId) { this.farmerId = farmerId; }
    public void setFarmerName(String farmerName) { this.farmerName = farmerName; }
    public void setFarmerCode(String farmerCode) { this.farmerCode = farmerCode; }
    public void setMobile(String mobile) { this.mobile = mobile; }
    public void setPeriodFrom(String periodFrom) { this.periodFrom = periodFrom; }
    public void setPeriodTo(String periodTo) { this.periodTo = periodTo; }
    public void setStatus(String status) { this.status = status; }
    public void setTotalLiters(double totalLiters) { this.totalLiters = totalLiters; }
    public void setMilkAmount(double milkAmount) { this.milkAmount = milkAmount; }
    public void setBonusAmount(double bonusAmount) { this.bonusAmount = bonusAmount; }
    public void setDeductionAmount(double deductionAmount) { this.deductionAmount = deductionAmount; }
    public void setNetPayable(double netPayable) { this.netPayable = netPayable; }
    public void setMilkSnapshot(List<Map<String, Object>> milkSnapshot) {
        this.milkSnapshot = milkSnapshot == null ? new ArrayList<>() : milkSnapshot;
    }

    public void setBonusSnapshot(List<Map<String, Object>> bonusSnapshot) {
        this.bonusSnapshot = bonusSnapshot == null ? new ArrayList<>() : bonusSnapshot;
    }

    public void setDeductionSnapshot(List<Map<String, Object>> deductionSnapshot) {
        this.deductionSnapshot = deductionSnapshot == null ? new ArrayList<>() : deductionSnapshot;
    }
}