package com.financal.mgt.Financal.Management.dto.response.account;

import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BankAccountResponse {
    private String bankId;
    private String bankAccountNumber;
    private String bankName;
    private String accountHolderName;
    private String accountStatus;
    private String bankCode;
    private String accountType;
    private LocalDateTime createdAt;
}
