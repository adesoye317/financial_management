package com.financal.mgt.Financal.Management.dto.response.auth;


import lombok.Data;

@Data
public class SignupRequest {
    private String email;
    private String password;
}

