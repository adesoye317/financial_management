package com.financal.mgt.Financal.Management.dto.response.insight;


import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InvestmentInsightResponse {
    private BigDecimal totalInvested;
    private BigDecimal currentValue;
    private BigDecimal totalReturns;
    private BigDecimal overallROI;
    private PoolPerformance bestPerforming;
    private PoolPerformance worstPerforming;
    private List<RiskDistribution> riskDistribution;
    private List<MonthlyReturn> monthlyReturns;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PoolPerformance {
        private String poolName;
        private BigDecimal returnPct;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class RiskDistribution {
        private String riskLevel;
        private BigDecimal amount;
        private BigDecimal percentage;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class MonthlyReturn {
        private String month;
        private BigDecimal returns;
    }
}
