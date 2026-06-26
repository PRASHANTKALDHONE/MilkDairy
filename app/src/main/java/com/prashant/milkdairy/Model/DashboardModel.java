package com.prashant.milkdairy.Model;

public class DashboardModel {

    private double milkLitersToday;
    private double milkAmountToday;
    private double inventoryValue;
    private double billsAmount;

    private int billsCount;
    private int activeFarmers;

    private double morningCow;
    private double morningBuffalo;
    private double morningMixed;
    private double morningTotal;

    private double eveningCow;
    private double eveningBuffalo;
    private double eveningMixed;
    private double eveningTotal;

    public void addMilkRecord(String shift, String milkType, double liters, double amount) {
        milkLitersToday += liters;
        milkAmountToday += amount;

        if ("Morning".equalsIgnoreCase(shift)) {
            morningTotal += liters;

            if (isCow(milkType)) {
                morningCow += liters;
            } else if (isBuffalo(milkType)) {
                morningBuffalo += liters;
            } else {
                morningMixed += liters;
            }
        }

        if ("Evening".equalsIgnoreCase(shift)) {
            eveningTotal += liters;

            if (isCow(milkType)) {
                eveningCow += liters;
            } else if (isBuffalo(milkType)) {
                eveningBuffalo += liters;
            } else {
                eveningMixed += liters;
            }
        }
    }

    public void addInventoryValue(double value) {
        inventoryValue += value;
    }

    public void addBill(double amount) {
        billsCount++;
        billsAmount += amount;
    }

    public void incrementActiveFarmers() {
        activeFarmers++;
    }

    private boolean isCow(String value) {
        return value != null && value.toLowerCase().contains("cow");
    }

    private boolean isBuffalo(String value) {
        return value != null && value.toLowerCase().contains("buffalo");
    }

    public double getMilkLitersToday() { return milkLitersToday; }
    public double getMilkAmountToday() { return milkAmountToday; }
    public double getInventoryValue() { return inventoryValue; }
    public double getBillsAmount() { return billsAmount; }

    public int getBillsCount() { return billsCount; }
    public int getActiveFarmers() { return activeFarmers; }

    public double getMorningCow() { return morningCow; }
    public double getMorningBuffalo() { return morningBuffalo; }
    public double getMorningMixed() { return morningMixed; }
    public double getMorningTotal() { return morningTotal; }

    public double getEveningCow() { return eveningCow; }
    public double getEveningBuffalo() { return eveningBuffalo; }
    public double getEveningMixed() { return eveningMixed; }
    public double getEveningTotal() { return eveningTotal; }
}