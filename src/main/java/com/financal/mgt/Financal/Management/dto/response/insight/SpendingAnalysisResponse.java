package com.financal.mgt.Financal.Management.dto.response.insight;


import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SpendingAnalysisResponse {
    private BigDecimal totalSpent;
    private BigDecimal averageMonthly;
    private MonthAmount highestMonth;
    private MonthAmount lowestMonth;
    private List<CategoryBreakdown> byCategory;
    private List<MonthlyTrend> monthlyTrend;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class MonthAmount {
        private String month;
        private BigDecimal amount;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CategoryBreakdown {
        private String category;
        private BigDecimal amount;
        private BigDecimal percentage;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class MonthlyTrend {
        private String month;
        private BigDecimal income;
        private BigDecimal expenses;
        private BigDecimal net;
    }
}
