package com.financal.mgt.Financal.Management.dto.response.insight;


import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class IncomeAnalysisResponse {
    private BigDecimal totalIncome;
    private BigDecimal averageMonthly;
    private List<SourceBreakdown> bySource;
    private List<MonthlyAmount> monthlyTrend;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SourceBreakdown {
        private String source;
        private BigDecimal amount;
        private BigDecimal percentage;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class MonthlyAmount {
        private String month;
        private BigDecimal amount;
    }
}
