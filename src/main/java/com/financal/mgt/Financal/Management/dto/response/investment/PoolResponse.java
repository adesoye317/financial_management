package com.financal.mgt.Financal.Management.dto.response.investment;


import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PoolResponse {
    private Long id;
    private String poolName;
    private String description;
    private String riskLevel;
    private BigDecimal annualReturnPct;
    private BigDecimal minInvestment;
    private BigDecimal maxInvestment;
    private BigDecimal totalPoolSize;
    private BigDecimal currentInvested;
    private BigDecimal availableSlots;
    private int durationMonths;
    private String status;
    private LocalDate maturityDate;
    private String terms;
    private BigDecimal earlyWithdrawalPenaltyPct;
    private LocalDateTime createdAt;
}
