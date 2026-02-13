package com.financal.mgt.Financal.Management.dto.response.auth;


import lombok.Data;

@Data
public class BusinessSetupRequest {
    private String language;
    private String firstName;
    private String LastName;
    private String phoneNumber;
    private String address;
    private String accountType;
    private String goals;
    private String currency;
}

