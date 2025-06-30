package com.financal.mgt.Financal.Management.dto.request;

public class VerifyEmailRequest {

    private String otp;

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    @Override
    public String toString() {
        return "VerifyEmailRequest{" +
                "otp='" + otp + '\'' +
                '}';
    }
}
