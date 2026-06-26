package com.prashant.milkdairy.Model;

public class RecentActivity {

    private final String type;
    private final String title;
    private final String subtitle;
    private final String amount;
    private final String time;
    private final long millis;

    public RecentActivity(String type, String title, String subtitle, String amount, String time, long millis) {
        this.type = type;
        this.title = title;
        this.subtitle = subtitle;
        this.amount = amount;
        this.time = time;
        this.millis = millis;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getAmount() {
        return amount;
    }

    public String getTime() {
        return time;
    }

    public long getMillis() {
        return millis;
    }
}
