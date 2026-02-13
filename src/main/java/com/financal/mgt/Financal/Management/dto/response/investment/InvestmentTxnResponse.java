package com.financal.mgt.Financal.Management.dto.response.investment;


import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InvestmentTxnResponse {
    private String transactionRef;
    private String investmentRef;
    private BigDecimal amount;
    private String type;
    private String description;
    private LocalDateTime createdAt;
}
