package com.prashant.milkdairy.Model;

public class MilkCollectionModel {

    private String id, farmerId, date, shift, farmerCode, farmerName, mobile, milkType, remarks;
    private double liters, fat, snf, rate, total, grossAmount, deductedAmount, netPayable;
    private long dateMillis, createdAtMillis;

    public MilkCollectionModel() {}

    public String getId() { return id; }
    public String getFarmerId() { return farmerId; }
    public String getDate() { return date; }
    public String getShift() { return shift; }
    public String getFarmerCode() { return farmerCode; }
    public String getFarmerName() { return farmerName; }
    public String getMobile() { return mobile; }
    public String getMilkType() { return milkType; }
    public String getRemarks() { return remarks; }
    public double getLiters() { return liters; }
    public double getFat() { return fat; }
    public double getSnf() { return snf; }
    public double getRate() { return rate; }
    public double getTotal() { return total; }
    public double getGrossAmount() { return grossAmount; }
    public double getDeductedAmount() { return deductedAmount; }
    public double getNetPayable() { return netPayable; }
    public long getDateMillis() { return dateMillis; }
    public long getCreatedAtMillis() { return createdAtMillis; }

    public void setId(String id) { this.id = id; }
    public void setFarmerId(String farmerId) { this.farmerId = farmerId; }
    public void setDate(String date) { this.date = date; }
    public void setShift(String shift) { this.shift = shift; }
    public void setFarmerCode(String farmerCode) { this.farmerCode = farmerCode; }
    public void setFarmerName(String farmerName) { this.farmerName = farmerName; }
    public void setMobile(String mobile) { this.mobile = mobile; }
    public void setMilkType(String milkType) { this.milkType = milkType; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public void setLiters(double liters) { this.liters = liters; }
    public void setFat(double fat) { this.fat = fat; }
    public void setSnf(double snf) { this.snf = snf; }
    public void setRate(double rate) { this.rate = rate; }
    public void setTotal(double total) { this.total = total; }
    public void setGrossAmount(double grossAmount) { this.grossAmount = grossAmount; }
    public void setDeductedAmount(double deductedAmount) { this.deductedAmount = deductedAmount; }
    public void setNetPayable(double netPayable) { this.netPayable = netPayable; }
    public void setDateMillis(long dateMillis) { this.dateMillis = dateMillis; }
    public void setCreatedAtMillis(long createdAtMillis) { this.createdAtMillis = createdAtMillis; }
}