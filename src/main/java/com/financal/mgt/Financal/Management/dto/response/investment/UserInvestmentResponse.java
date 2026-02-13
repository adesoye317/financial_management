package com.financal.mgt.Financal.Management.dto.response.investment;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserInvestmentResponse {
    private String investmentRef;
    private String poolName;
    private String riskLevel;
    private BigDecimal amountInvested;
    private BigDecimal currentValue;
    private BigDecimal returnsEarned;
    private BigDecimal returnPct;
    private BigDecimal annualReturnPct;
    private String status;
    private boolean autoReinvest;
    private LocalDateTime investedAt;
    private LocalDate maturityDate;
    private long daysRemaining;
}
