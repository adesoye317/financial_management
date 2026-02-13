package com.financal.mgt.Financal.Management.dto.response.investment;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PortfolioResponse {
    private BigDecimal totalInvested;
    private BigDecimal totalCurrentValue;
    private BigDecimal totalReturnsEarned;
    private BigDecimal overallReturnPct;
    private int activeInvestments;
    private int maturedInvestments;
    private List<UserInvestmentResponse> investments;
}
