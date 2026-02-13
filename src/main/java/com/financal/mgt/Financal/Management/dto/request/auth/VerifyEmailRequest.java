package com.financal.mgt.Financal.Management.dto.request.auth;

public class VerifyEmailRequest {

    private String otp;
    private String email;

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return "VerifyEmailRequest{" +
                "otp='" + otp + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
