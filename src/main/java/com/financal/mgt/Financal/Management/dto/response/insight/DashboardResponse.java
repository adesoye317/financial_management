package com.financal.mgt.Financal.Management.dto.response.insight;

import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DashboardResponse {
    private String period;
    private BigDecimal walletBalance;
    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
    private BigDecimal netSavings;
    private BigDecimal savingsRate;
    private BigDecimal investmentValue;
    private BigDecimal goalsProgress;
    private MonthOverMonth monthOverMonth;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class MonthOverMonth {
        private BigDecimal incomeChange;
        private BigDecimal expenseChange;
        private BigDecimal savingsChange;
    }
}