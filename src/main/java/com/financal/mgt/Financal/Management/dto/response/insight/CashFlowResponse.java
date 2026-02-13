package com.financal.mgt.Financal.Management.dto.response.insight;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CashFlowResponse {
    private BigDecimal currentBalance;
    private BigDecimal projectedBalance;
    private BigDecimal averageMonthlyInflow;
    private BigDecimal averageMonthlyOutflow;
    private List<MonthForecast> forecast;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class MonthForecast {
        private String month;
        private BigDecimal projectedIncome;
        private BigDecimal projectedExpenses;
        private BigDecimal projectedBalance;
        private BigDecimal confidence;
    }
}
