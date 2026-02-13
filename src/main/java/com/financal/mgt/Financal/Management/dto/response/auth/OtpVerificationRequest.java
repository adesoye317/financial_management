package com.financal.mgt.Financal.Management.dto.response.auth;


import lombok.Data;

@Data
public class OtpVerificationRequest {
    private String email;
    private String otp;
}
