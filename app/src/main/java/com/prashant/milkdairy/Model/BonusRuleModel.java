package com.prashant.milkdairy.Model;

public class BonusRuleModel {

    private String ruleId;
    private String ruleName;
    private String bonusType;
    private double value;
    private double perAmount;
    private double minLiter;
    private String milkType;
    private boolean active;
    private String description;

    public BonusRuleModel() {}

    public String getRuleId() { return ruleId; }
    public String getRuleName() { return ruleName; }
    public String getBonusType() { return bonusType; }
    public double getValue() { return value; }
    public double getPerAmount() { return perAmount; }
    public double getMinLiter() { return minLiter; }
    public String getMilkType() { return milkType; }
    public boolean isActive() { return active; }
    public String getDescription() { return description; }

    public void setRuleId(String ruleId) { this.ruleId = ruleId; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }
    public void setBonusType(String bonusType) { this.bonusType = bonusType; }
    public void setValue(double value) { this.value = value; }
    public void setPerAmount(double perAmount) { this.perAmount = perAmount; }
    public void setMinLiter(double minLiter) { this.minLiter = minLiter; }
    public void setMilkType(String milkType) { this.milkType = milkType; }
    public void setActive(boolean active) { this.active = active; }
    public void setDescription(String description) { this.description = description; }
}
