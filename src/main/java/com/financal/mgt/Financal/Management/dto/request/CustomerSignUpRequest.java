package com.financal.mgt.Financal.Management.dto.request;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class CustomerSignUpRequest {
    @NotBlank
    private String phoneNumber;

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String password;
    private String deviceToken;


    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDeviceToken() {
        return deviceToken;
    }

    public void setDeviceToken(String deviceToken) {
        this.deviceToken = deviceToken;
    }

    @Override
    public String toString() {
        return "CustomerSignUpRequest{" +
                "phoneNumber='" + phoneNumber + '\'' +
                ", email='" + email + '\'' +
                ", password='" + password + '\'' +
                ", deviceToken='" + deviceToken + '\'' +
                '}';
    }
}

