package com.prashant.milkdairy.Model;

public class Farmer {

    private String id;
    private String code;
    private String name;
    private String mobile;
    private String milkType;
    private String status;
    private long createdAtMillis;
    private long updatedAtMillis;

    public Farmer() {
    }

    public Farmer(String id,
                  String code,
                  String name,
                  String mobile,
                  String milkType,
                  String status,
                  long createdAtMillis,
                  long updatedAtMillis) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.mobile = mobile;
        this.milkType = milkType;
        this.status = status;
        this.createdAtMillis = createdAtMillis;
        this.updatedAtMillis = updatedAtMillis;
    }

    public String getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getMobile() {
        return mobile;
    }

    public String getMilkType() {
        return milkType;
    }

    public String getStatus() {
        return status;
    }

    public long getCreatedAtMillis() {
        return createdAtMillis;
    }

    public long getUpdatedAtMillis() {
        return updatedAtMillis;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public void setMilkType(String milkType) {
        this.milkType = milkType;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCreatedAtMillis(long createdAtMillis) {
        this.createdAtMillis = createdAtMillis;
    }

    public void setUpdatedAtMillis(long updatedAtMillis) {
        this.updatedAtMillis = updatedAtMillis;
    }
}