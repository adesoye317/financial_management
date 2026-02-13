package com.financal.mgt.Financal.Management.dto.response.wallet;

import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WalletResponse {
    private String accountNumber;
    private BigDecimal balance;
    private String currency;
    private String accountHolderName;
}
