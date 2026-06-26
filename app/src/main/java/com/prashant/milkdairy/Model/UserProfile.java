package com.prashant.milkdairy.Model;
public class UserProfile {

    private String userId;
    private String fullName;
    private String email;
    private String mobileNumber;
    private String whatsappNumber;
    private String dairyName;
    private String dairyAddress;

    public UserProfile() {
        // Required empty constructor for Firestore
    }

    public UserProfile(String userId, String fullName, String email,
                       String mobileNumber, String whatsappNumber,
                       String dairyName, String dairyAddress) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.mobileNumber = mobileNumber;
        this.whatsappNumber = whatsappNumber;
        this.dairyName = dairyName;
        this.dairyAddress = dairyAddress;
    }

    public String getUserId() {
        return userId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public String getWhatsappNumber() {
        return whatsappNumber;
    }

    public String getDairyName() {
        return dairyName;
    }

    public String getDairyAddress() {
        return dairyAddress;
    }
}

