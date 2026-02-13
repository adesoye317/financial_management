package com.financal.mgt.Financal.Management.dto.request.account;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class UpdateBankAccountRequest {
    private String bankName;
    private String accountHolderName;
    private String bankCode;
    private String accountType;
    private String bankId;
}
