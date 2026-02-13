package com.financal.mgt.Financal.Management.dto.response.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {
    private String id;
    private String email;
    private boolean verified;
    private String profileSetupStatus;
    private String language;
    private String firstName;
    private String LastName;
    private String phoneNumber;
    private String address;
    private String accountType;
    private String goals;
    private String currency;
}