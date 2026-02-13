package com.financal.mgt.Financal.Management.dto.response.investment;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InvestResultResponse {
    private String investmentRef;
    private String poolName;
    private BigDecimal amountInvested;
    private BigDecimal currentValue;
    private BigDecimal annualReturnPct;
    private String status;
    private LocalDateTime investedAt;
    private LocalDate maturityDate;
    private boolean autoReinvest;
    private BigDecimal walletBalanceAfter;
}
