package com.prashant.milkdairy.Model;

public class FarmerStat {

    private String title;
    private String value;
    private int backgroundColor;

    public FarmerStat(String title, String value, int backgroundColor) {
        this.title = title;
        this.value = value;
        this.backgroundColor = backgroundColor;
    }

    public String getTitle() {
        return title;
    }

    public String getValue() {
        return value;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }
}