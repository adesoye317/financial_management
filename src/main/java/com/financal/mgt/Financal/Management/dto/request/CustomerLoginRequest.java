package com.financal.mgt.Financal.Management.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class CustomerLoginRequest {

    @Email
    @NotBlank
    private String username;
    @NotBlank
    private String password;
    @NotBlank
    private String deviceToken;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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
        return "CustomerLoginRequest{" +
                "username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", deviceToken='" + deviceToken + '\'' +
                '}';
    }
}
