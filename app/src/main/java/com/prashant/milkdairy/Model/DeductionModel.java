package com.prashant.milkdairy.Model;

public class DeductionModel {

    private String id;
    private String farmerId;
    private String farmerCode;
    private String farmerName;
    private String date;
    private String category;
    private String status;
    private String description;

    private long dateMillis;
    private long createdAtMillis;
    private long updatedAtMillis;

    private double amount;
    private double remaining;

    public DeductionModel() {
    }

    public String getId() {
        return id;
    }

    public String getFarmerId() {
        return farmerId;
    }

    public String getFarmerCode() {
        return farmerCode;
    }

    public String getFarmerName() {
        return farmerName;
    }

    public String getDate() {
        return date;
    }

    public String getCategory() {
        return category;
    }

    public String getStatus() {
        return status;
    }

    public String getDescription() {
        return description;
    }

    public long getDateMillis() {
        return dateMillis;
    }

    public long getCreatedAtMillis() {
        return createdAtMillis;
    }

    public long getUpdatedAtMillis() {
        return updatedAtMillis;
    }

    public double getAmount() {
        return amount;
    }

    public double getRemaining() {
        return remaining;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setFarmerId(String farmerId) {
        this.farmerId = farmerId;
    }

    public void setFarmerCode(String farmerCode) {
        this.farmerCode = farmerCode;
    }

    public void setFarmerName(String farmerName) {
        this.farmerName = farmerName;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDateMillis(long dateMillis) {
        this.dateMillis = dateMillis;
    }

    public void setCreatedAtMillis(long createdAtMillis) {
        this.createdAtMillis = createdAtMillis;
    }

    public void setUpdatedAtMillis(long updatedAtMillis) {
        this.updatedAtMillis = updatedAtMillis;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public void setRemaining(double remaining) {
        this.remaining = remaining;
    }
}