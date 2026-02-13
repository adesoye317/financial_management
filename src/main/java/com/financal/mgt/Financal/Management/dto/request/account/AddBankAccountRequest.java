package com.financal.mgt.Financal.Management.dto.request.account;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class AddBankAccountRequest {

    @NotBlank(message = "Bank account number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Account number must be 10 digits")
    private String bankAccountNumber;

    @NotBlank(message = "Bank name is required")
    private String bankName;

    @NotBlank(message = "Account holder name is required")
    private String accountHolderName;

    private String bankCode;
    private String accountType; // SAVINGS, CURRENT
}
