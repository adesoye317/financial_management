package com.financal.mgt.Financal.Management.service.insight;


import com.financal.mgt.Financal.Management.dto.response.auth.ApiResponse;
import com.financal.mgt.Financal.Management.dto.response.insight.*;
import com.financal.mgt.Financal.Management.enums.goal.GoalStatus;
import com.financal.mgt.Financal.Management.model.goal.FinancialGoal;
import com.financal.mgt.Financal.Management.model.wallet.Wallet;
import com.financal.mgt.Financal.Management.model.wallet.WalletTransaction;
import com.financal.mgt.Financal.Management.repository.goal.FinancialGoalRepository;
import com.financal.mgt.Financal.Management.repository.wallet.WalletRepository;
import com.financal.mgt.Financal.Management.repository.wallet.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InsightServiceImpl implements InsightService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final FinancialGoalRepository goalRepository;

    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("MMM yyyy");
    private static final Set<String> INCOME_TYPES = Set.of("FUNDING", "INVESTMENT_CREDIT");
    private static final Set<String> EXPENSE_TYPES = Set.of("TRANSFER", "WITHDRAWAL", "INVESTMENT_DEBIT");

    // ==================== DASHBOARD ====================

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<DashboardResponse> getDashboard(String userId) {
        try {
            Wallet wallet = walletRepository.findByUserId(userId).orElse(null);
            if (wallet == null) {
                return new ApiResponse<>(404, "Wallet not found", null);
            }

            String accNo = wallet.getAccountNumber();
            YearMonth currentMonth = YearMonth.now();
            YearMonth prevMonth = currentMonth.minusMonths(1);

            LocalDateTime currentStart = currentMonth.atDay(1).atStartOfDay();
            LocalDateTime currentEnd = LocalDateTime.now();
            LocalDateTime prevStart = prevMonth.atDay(1).atStartOfDay();
            LocalDateTime prevEnd = prevMonth.atEndOfMonth().atTime(LocalTime.MAX);

            BigDecimal currentIncome = transactionRepository.getIncomeForPeriod(accNo, currentStart, currentEnd);
            BigDecimal currentExpenses = transactionRepository.getExpensesForPeriod(accNo, currentStart, currentEnd);
            BigDecimal prevIncome = transactionRepository.getIncomeForPeriod(accNo, prevStart, prevEnd);
            BigDecimal prevExpenses = transactionRepository.getExpensesForPeriod(accNo, prevStart, prevEnd);

            BigDecimal netSavings = currentIncome.subtract(currentExpenses);
            BigDecimal savingsRate = currentIncome.compareTo(BigDecimal.ZERO) > 0
                    ? netSavings.multiply(new BigDecimal("100")).divide(currentIncome, 1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            BigDecimal prevNet = prevIncome.subtract(prevExpenses);

            BigDecimal goalsProgress = BigDecimal.ZERO;
            BigDecimal totalTarget = goalRepository.getTotalTargetForActive(userId);
            BigDecimal totalSaved = goalRepository.getTotalSaved(userId);
            if (totalTarget.compareTo(BigDecimal.ZERO) > 0) {
                goalsProgress = totalSaved.multiply(new BigDecimal("100"))
                        .divide(totalTarget, 1, RoundingMode.HALF_UP);
            }

            DashboardResponse dashboard = DashboardResponse.builder()
                    .period(currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")))
                    .walletBalance(wallet.getBalance())
                    .totalIncome(currentIncome)
                    .totalExpenses(currentExpenses)
                    .netSavings(netSavings)
                    .savingsRate(savingsRate)
                    .investmentValue(BigDecimal.ZERO) // TODO: integrate with investment module
                    .goalsProgress(goalsProgress)
                    .monthOverMonth(DashboardResponse.MonthOverMonth.builder()
                            .incomeChange(calcPercentChange(prevIncome, currentIncome))
                            .expenseChange(calcPercentChange(prevExpenses, currentExpenses))
                            .savingsChange(calcPercentChange(prevNet, netSavings))
                            .build())
                    .build();

            return new ApiResponse<>(200, "Dashboard insights retrieved", dashboard);

        } catch (Exception e) {
            log.error("Failed to get dashboard: {}", e.getMessage(), e);
            return new ApiResponse<>(500, "Unable to retrieve dashboard. Please try again later.", null);
        }
    }

    // ==================== SPENDING ANALYSIS ====================

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<SpendingAnalysisResponse> getSpendingAnalysis(String userId, String period, int months) {
        try {
            Wallet wallet = walletRepository.findByUserId(userId).orElse(null);
            if (wallet == null) {
                return new ApiResponse<>(404, "Wallet not found", null);
            }

            String accNo = wallet.getAccountNumber();
            YearMonth now = YearMonth.now();
            LocalDateTime start = now.minusMonths(months - 1).atDay(1).atStartOfDay();
            LocalDateTime end = LocalDateTime.now();

            // Get all transactions for the period
            List<WalletTransaction> allTxns = transactionRepository.findSuccessfulForPeriod(accNo, start, end);

            // Separate income and expenses
            List<WalletTransaction> expenseTxns = allTxns.stream()
                    .filter(t -> EXPENSE_TYPES.contains(t.getType().name()))
                    .toList();

            BigDecimal totalSpent = expenseTxns.stream()
                    .map(WalletTransaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal averageMonthly = months > 0
                    ? totalSpent.divide(new BigDecimal(months), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            // By category (transaction type)
            Map<String, BigDecimal> categoryMap = expenseTxns.stream()
                    .collect(Collectors.groupingBy(
                            t -> t.getType().name(),
                            Collectors.reducing(BigDecimal.ZERO, WalletTransaction::getAmount, BigDecimal::add)));

            List<SpendingAnalysisResponse.CategoryBreakdown> byCategory = categoryMap.entrySet().stream()
                    .map(e -> SpendingAnalysisResponse.CategoryBreakdown.builder()
                            .category(e.getKey())
                            .amount(e.getValue())
                            .percentage(totalSpent.compareTo(BigDecimal.ZERO) > 0
                                    ? e.getValue().multiply(new BigDecimal("100"))
                                    .divide(totalSpent, 1, RoundingMode.HALF_UP)
                                    : BigDecimal.ZERO)
                            .build())
                    .sorted((a, b) -> b.getAmount().compareTo(a.getAmount()))
                    .toList();

            // Monthly trend
            List<SpendingAnalysisResponse.MonthlyTrend> monthlyTrend = new ArrayList<>();
            SpendingAnalysisResponse.MonthAmount highest = null;
            SpendingAnalysisResponse.MonthAmount lowest = null;

            for (int i = months - 1; i >= 0; i--) {
                YearMonth ym = now.minusMonths(i);
                LocalDateTime mStart = ym.atDay(1).atStartOfDay();
                LocalDateTime mEnd = ym.equals(now) ? LocalDateTime.now() : ym.atEndOfMonth().atTime(LocalTime.MAX);

                BigDecimal mIncome = transactionRepository.getIncomeForPeriod(accNo, mStart, mEnd);
                BigDecimal mExpense = transactionRepository.getExpensesForPeriod(accNo, mStart, mEnd);
                String monthLabel = ym.format(MONTH_FORMAT);

                monthlyTrend.add(SpendingAnalysisResponse.MonthlyTrend.builder()
                        .month(monthLabel)
                        .income(mIncome)
                        .expenses(mExpense)
                        .net(mIncome.subtract(mExpense))
                        .build());

                if (highest == null || mExpense.compareTo(highest.getAmount()) > 0) {
                    highest = new SpendingAnalysisResponse.MonthAmount(monthLabel, mExpense);
                }
                if (lowest == null || mExpense.compareTo(lowest.getAmount()) < 0) {
                    lowest = new SpendingAnalysisResponse.MonthAmount(monthLabel, mExpense);
                }
            }

            SpendingAnalysisResponse response = SpendingAnalysisResponse.builder()
                    .totalSpent(totalSpent)
                    .averageMonthly(averageMonthly)
                    .highestMonth(highest)
                    .lowestMonth(lowest)
                    .byCategory(byCategory)
                    .monthlyTrend(monthlyTrend)
                    .build();

            return new ApiResponse<>(200, "Spending analysis retrieved", response);

        } catch (Exception e) {
            log.error("Failed to get spending analysis: {}", e.getMessage(), e);
            return new ApiResponse<>(500, "Unable to retrieve spending analysis.", null);
        }
    }

    // ==================== INCOME ANALYSIS ====================

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<IncomeAnalysisResponse> getIncomeAnalysis(String userId, String period, int months) {
        try {
            Wallet wallet = walletRepository.findByUserId(userId).orElse(null);
            if (wallet == null) {
                return new ApiResponse<>(404, "Wallet not found", null);
            }

            String accNo = wallet.getAccountNumber();
            YearMonth now = YearMonth.now();
            LocalDateTime start = now.minusMonths(months - 1).atDay(1).atStartOfDay();
            LocalDateTime end = LocalDateTime.now();

            List<WalletTransaction> allTxns = transactionRepository.findSuccessfulForPeriod(accNo, start, end);

            List<WalletTransaction> incomeTxns = allTxns.stream()
                    .filter(t -> INCOME_TYPES.contains(t.getType().name()))
                    .toList();

            BigDecimal totalIncome = incomeTxns.stream()
                    .map(WalletTransaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal averageMonthly = months > 0
                    ? totalIncome.divide(new BigDecimal(months), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            // By source
            Map<String, BigDecimal> sourceMap = incomeTxns.stream()
                    .collect(Collectors.groupingBy(
                            t -> t.getType().name(),
                            Collectors.reducing(BigDecimal.ZERO, WalletTransaction::getAmount, BigDecimal::add)));

            List<IncomeAnalysisResponse.SourceBreakdown> bySource = sourceMap.entrySet().stream()
                    .map(e -> IncomeAnalysisResponse.SourceBreakdown.builder()
                            .source(e.getKey())
                            .amount(e.getValue())
                            .percentage(totalIncome.compareTo(BigDecimal.ZERO) > 0
                                    ? e.getValue().multiply(new BigDecimal("100"))
                                    .divide(totalIncome, 1, RoundingMode.HALF_UP)
                                    : BigDecimal.ZERO)
                            .build())
                    .sorted((a, b) -> b.getAmount().compareTo(a.getAmount()))
                    .toList();

            // Monthly trend
            List<IncomeAnalysisResponse.MonthlyAmount> monthlyTrend = new ArrayList<>();
            for (int i = months - 1; i >= 0; i--) {
                YearMonth ym = now.minusMonths(i);
                LocalDateTime mStart = ym.atDay(1).atStartOfDay();
                LocalDateTime mEnd = ym.equals(now) ? LocalDateTime.now() : ym.atEndOfMonth().atTime(LocalTime.MAX);
                BigDecimal mIncome = transactionRepository.getIncomeForPeriod(accNo, mStart, mEnd);
                monthlyTrend.add(IncomeAnalysisResponse.MonthlyAmount.builder()
                        .month(ym.format(MONTH_FORMAT))
                        .amount(mIncome)
                        .build());
            }

            IncomeAnalysisResponse response = IncomeAnalysisResponse.builder()
                    .totalIncome(totalIncome)
                    .averageMonthly(averageMonthly)
                    .bySource(bySource)
                    .monthlyTrend(monthlyTrend)
                    .build();

            return new ApiResponse<>(200, "Income analysis retrieved", response);

        } catch (Exception e) {
            log.error("Failed to get income analysis: {}", e.getMessage(), e);
            return new ApiResponse<>(500, "Unable to retrieve income analysis.", null);
        }
    }

    // ==================== INVESTMENT INSIGHTS ====================

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<InvestmentInsightResponse> getInvestmentInsights(String userId) {
        try {
            // TODO: Integrate with InvestmentRepository once investment module is built
            // For now, derive from wallet transactions

            Wallet wallet = walletRepository.findByUserId(userId).orElse(null);
            if (wallet == null) {
                return new ApiResponse<>(404, "Wallet not found", null);
            }

            String accNo = wallet.getAccountNumber();
            LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);

            List<WalletTransaction> allTxns = transactionRepository
                    .findSuccessfulForPeriod(accNo, sixMonthsAgo, LocalDateTime.now());

            BigDecimal totalInvested = allTxns.stream()
                    .filter(t -> "INVESTMENT_DEBIT".equals(t.getType().name()))
                    .map(WalletTransaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalReturns = allTxns.stream()
                    .filter(t -> "INVESTMENT_CREDIT".equals(t.getType().name()))
                    .map(WalletTransaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal currentValue = totalInvested.add(totalReturns);
            BigDecimal overallROI = totalInvested.compareTo(BigDecimal.ZERO) > 0
                    ? totalReturns.multiply(new BigDecimal("100"))
                    .divide(totalInvested, 1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            // Monthly returns trend
            YearMonth now = YearMonth.now();
            List<InvestmentInsightResponse.MonthlyReturn> monthlyReturns = new ArrayList<>();
            for (int i = 5; i >= 0; i--) {
                YearMonth ym = now.minusMonths(i);
                LocalDateTime mStart = ym.atDay(1).atStartOfDay();
                LocalDateTime mEnd = ym.equals(now) ? LocalDateTime.now() : ym.atEndOfMonth().atTime(LocalTime.MAX);

                BigDecimal mReturns = allTxns.stream()
                        .filter(t -> "INVESTMENT_CREDIT".equals(t.getType().name()))
                        .filter(t -> !t.getCreatedAt().isBefore(mStart) && !t.getCreatedAt().isAfter(mEnd))
                        .map(WalletTransaction::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                monthlyReturns.add(InvestmentInsightResponse.MonthlyReturn.builder()
                        .month(ym.format(MONTH_FORMAT))
                        .returns(mReturns)
                        .build());
            }

            InvestmentInsightResponse response = InvestmentInsightResponse.builder()
                    .totalInvested(totalInvested)
                    .currentValue(currentValue)
                    .totalReturns(totalReturns)
                    .overallROI(overallROI)
                    .riskDistribution(new ArrayList<>()) // Populated when investment module is integrated
                    .monthlyReturns(monthlyReturns)
                    .build();

            return new ApiResponse<>(200, "Investment insights retrieved", response);

        } catch (Exception e) {
            log.error("Failed to get investment insights: {}", e.getMessage(), e);
            return new ApiResponse<>(500, "Unable to retrieve investment insights.", null);
        }
    }

    // ==================== GOAL INSIGHTS ====================

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<GoalInsightResponse> getGoalInsights(String userId) {
        try {
            List<FinancialGoal> goals = goalRepository.findByUserIdOrderByCreatedAtDesc(userId);

            int activeGoals = (int) goals.stream().filter(g -> g.getStatus() == GoalStatus.ACTIVE).count();
            int achievedGoals = (int) goals.stream().filter(g -> g.getStatus() == GoalStatus.ACHIEVED).count();

            BigDecimal totalSaved = goalRepository.getTotalSaved(userId);
            BigDecimal totalTarget = goalRepository.getTotalTargetForActive(userId);

            BigDecimal overallProgress = totalTarget.compareTo(BigDecimal.ZERO) > 0
                    ? totalSaved.multiply(new BigDecimal("100"))
                    .divide(totalTarget, 1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            int onTrack = 0;
            int behindSchedule = 0;
            List<GoalInsightResponse.GoalDetail> goalDetails = new ArrayList<>();

            for (FinancialGoal goal : goals) {
                if (goal.getStatus() != GoalStatus.ACTIVE) continue;

                BigDecimal remaining = goal.getRemainingAmount();
                long monthsLeft = ChronoUnit.MONTHS.between(LocalDate.now(), goal.getTargetDate());
                if (monthsLeft <= 0) monthsLeft = 1;

                BigDecimal monthlyRequired = remaining.divide(new BigDecimal(monthsLeft), 2, RoundingMode.HALF_UP);

                // Estimate current monthly saving (saved / months since creation)
                long monthsSinceCreation = ChronoUnit.MONTHS.between(
                        goal.getCreatedAt().toLocalDate(), LocalDate.now());
                if (monthsSinceCreation <= 0) monthsSinceCreation = 1;

                BigDecimal currentMonthlySaving = goal.getSavedAmount()
                        .divide(new BigDecimal(monthsSinceCreation), 2, RoundingMode.HALF_UP);

                boolean isOnTrack = currentMonthlySaving.compareTo(monthlyRequired) >= 0;
                if (isOnTrack) onTrack++;
                else behindSchedule++;

                BigDecimal diff = currentMonthlySaving.subtract(monthlyRequired);

                // Project completion date
                String projectedCompletion;
                if (currentMonthlySaving.compareTo(BigDecimal.ZERO) > 0) {
                    long monthsToComplete = remaining.divide(currentMonthlySaving, 0, RoundingMode.CEILING).longValue();
                    projectedCompletion = LocalDate.now().plusMonths(monthsToComplete).toString();
                } else {
                    projectedCompletion = "N/A";
                }

                goalDetails.add(GoalInsightResponse.GoalDetail.builder()
                        .goalName(goal.getGoalName())
                        .progress(goal.getProgressPercentage())
                        .status(isOnTrack ? "ON_TRACK" : "BEHIND_SCHEDULE")
                        .monthlyRequired(monthlyRequired)
                        .currentMonthlySaving(currentMonthlySaving)
                        .shortfallOrSurplus(diff)
                        .projectedCompletion(projectedCompletion)
                        .build());
            }

            GoalInsightResponse response = GoalInsightResponse.builder()
                    .totalGoals(goals.size())
                    .activeGoals(activeGoals)
                    .achievedGoals(achievedGoals)
                    .overallProgress(overallProgress)
                    .totalSaved(totalSaved)
                    .totalTarget(totalTarget)
                    .onTrackGoals(onTrack)
                    .behindScheduleGoals(behindSchedule)
                    .goalDetails(goalDetails)
                    .build();

            return new ApiResponse<>(200, "Goal insights retrieved", response);

        } catch (Exception e) {
            log.error("Failed to get goal insights: {}", e.getMessage(), e);
            return new ApiResponse<>(500, "Unable to retrieve goal insights.", null);
        }
    }

    // ==================== RECOMMENDATIONS ====================

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<RecommendationResponse> getRecommendations(String userId) {
        try {
            List<RecommendationResponse.Recommendation> recommendations = new ArrayList<>();
            int id = 1;

            Wallet wallet = walletRepository.findByUserId(userId).orElse(null);
            if (wallet == null) {
                return new ApiResponse<>(404, "Wallet not found", null);
            }

            String accNo = wallet.getAccountNumber();
            YearMonth now = YearMonth.now();
            YearMonth prev = now.minusMonths(1);

            LocalDateTime currentStart = now.atDay(1).atStartOfDay();
            LocalDateTime prevStart = prev.atDay(1).atStartOfDay();
            LocalDateTime prevEnd = prev.atEndOfMonth().atTime(LocalTime.MAX);

            BigDecimal currentIncome = transactionRepository.getIncomeForPeriod(accNo, currentStart, LocalDateTime.now());
            BigDecimal currentExpenses = transactionRepository.getExpensesForPeriod(accNo, currentStart, LocalDateTime.now());
            BigDecimal prevExpenses = transactionRepository.getExpensesForPeriod(accNo, prevStart, prevEnd);

            // 1. Savings rate recommendation
            BigDecimal savingsRate = currentIncome.compareTo(BigDecimal.ZERO) > 0
                    ? currentIncome.subtract(currentExpenses).multiply(new BigDecimal("100"))
                    .divide(currentIncome, 1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            if (savingsRate.compareTo(new BigDecimal("20")) < 0) {
                recommendations.add(RecommendationResponse.Recommendation.builder()
                        .id(id++).type("SAVINGS").priority("HIGH")
                        .title("Low savings rate")
                        .message("Your savings rate is " + savingsRate + "%. Financial experts recommend saving at least 20% of income. Consider reducing non-essential spending.")
                        .actionable(true).action("VIEW_SPENDING").build());
            } else if (savingsRate.compareTo(new BigDecimal("40")) > 0) {
                recommendations.add(RecommendationResponse.Recommendation.builder()
                        .id(id++).type("SAVINGS").priority("LOW")
                        .title("Excellent savings rate!")
                        .message("Your savings rate is " + savingsRate + "%. You're saving well above the recommended 20%. Consider investing the surplus for higher returns.")
                        .actionable(true).action("VIEW_INVESTMENT_POOLS").build());
            }

            // 2. Spending trend
            if (prevExpenses.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal expenseChange = calcPercentChange(prevExpenses, currentExpenses);
                if (expenseChange.compareTo(new BigDecimal("15")) > 0) {
                    recommendations.add(RecommendationResponse.Recommendation.builder()
                            .id(id++).type("SPENDING").priority("HIGH")
                            .title("Spending spike detected")
                            .message("Your spending increased by " + expenseChange + "% compared to last month. Review your recent transactions to identify the cause.")
                            .actionable(true).action("VIEW_TRANSACTIONS").build());
                } else if (expenseChange.compareTo(BigDecimal.ZERO) < 0) {
                    recommendations.add(RecommendationResponse.Recommendation.builder()
                            .id(id++).type("SPENDING").priority("LOW")
                            .title("Spending trend positive")
                            .message("Your expenses decreased by " + expenseChange.abs() + "% this month. Great job keeping spending in check!")
                            .actionable(false).action(null).build());
                }
            }

            // 3. Goal recommendations
            List<FinancialGoal> activeGoals = goalRepository.findByUserIdAndStatus(userId, GoalStatus.ACTIVE);
            for (FinancialGoal goal : activeGoals) {
                long monthsLeft = ChronoUnit.MONTHS.between(LocalDate.now(), goal.getTargetDate());
                if (monthsLeft <= 0) monthsLeft = 1;

                BigDecimal monthlyRequired = goal.getRemainingAmount()
                        .divide(new BigDecimal(monthsLeft), 2, RoundingMode.HALF_UP);

                long monthsSinceCreation = ChronoUnit.MONTHS.between(
                        goal.getCreatedAt().toLocalDate(), LocalDate.now());
                if (monthsSinceCreation <= 0) monthsSinceCreation = 1;

                BigDecimal currentMonthly = goal.getSavedAmount()
                        .divide(new BigDecimal(monthsSinceCreation), 2, RoundingMode.HALF_UP);

                if (currentMonthly.compareTo(monthlyRequired) < 0) {
                    BigDecimal shortfall = monthlyRequired.subtract(currentMonthly);
                    recommendations.add(RecommendationResponse.Recommendation.builder()
                            .id(id++).type("GOAL").priority("HIGH")
                            .title(goal.getGoalName() + " behind schedule")
                            .message("Increase monthly contributions by ₦" + shortfall.toPlainString()
                                    + " (to ₦" + monthlyRequired.toPlainString()
                                    + "/month) to reach your target by " + goal.getTargetDate() + ".")
                            .actionable(true).action("FUND_GOAL").build());
                }

                // Warn if target date is within 30 days
                if (monthsLeft <= 1 && goal.getProgressPercentage().compareTo(new BigDecimal("80")) < 0) {
                    recommendations.add(RecommendationResponse.Recommendation.builder()
                            .id(id++).type("GOAL").priority("HIGH")
                            .title(goal.getGoalName() + " deadline approaching")
                            .message("Only " + monthsLeft + " month(s) left but you're at "
                                    + goal.getProgressPercentage() + "%. Consider extending the deadline or adding a lump sum.")
                            .actionable(true).action("EDIT_GOAL").build());
                }
            }

            // 4. No goals recommendation
            if (activeGoals.isEmpty()) {
                recommendations.add(RecommendationResponse.Recommendation.builder()
                        .id(id++).type("GOAL").priority("MEDIUM")
                        .title("Set a financial goal")
                        .message("You don't have any active savings goals. Setting goals helps you save with purpose and track progress.")
                        .actionable(true).action("CREATE_GOAL").build());
            }

            // 5. Low wallet balance warning
            if (wallet.getBalance().compareTo(new BigDecimal("5000")) < 0) {
                recommendations.add(RecommendationResponse.Recommendation.builder()
                        .id(id++).type("SAVINGS").priority("MEDIUM")
                        .title("Low wallet balance")
                        .message("Your wallet balance is ₦" + wallet.getBalance().toPlainString()
                                + ". Consider funding your wallet to cover upcoming expenses.")
                        .actionable(true).action("FUND_WALLET").build());
            }

            // Sort by priority
            Map<String, Integer> priorityOrder = Map.of("HIGH", 0, "MEDIUM", 1, "LOW", 2);
            recommendations.sort(Comparator.comparingInt(r -> priorityOrder.getOrDefault(r.getPriority(), 3)));

            return new ApiResponse<>(200, "Recommendations generated",
                    RecommendationResponse.builder().recommendations(recommendations).build());

        } catch (Exception e) {
            log.error("Failed to generate recommendations: {}", e.getMessage(), e);
            return new ApiResponse<>(500, "Unable to generate recommendations.", null);
        }
    }

    // ==================== CASH FLOW FORECAST ====================

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<CashFlowResponse> getCashFlowForecast(String userId, int forecastMonths) {
        try {
            Wallet wallet = walletRepository.findByUserId(userId).orElse(null);
            if (wallet == null) {
                return new ApiResponse<>(404, "Wallet not found", null);
            }

            String accNo = wallet.getAccountNumber();
            YearMonth now = YearMonth.now();

            // Analyze last 3 months for averages
            int analysisMonths = 3;
            BigDecimal totalInflow = BigDecimal.ZERO;
            BigDecimal totalOutflow = BigDecimal.ZERO;
            List<BigDecimal> monthlyInflows = new ArrayList<>();
            List<BigDecimal> monthlyOutflows = new ArrayList<>();

            for (int i = analysisMonths; i >= 1; i--) {
                YearMonth ym = now.minusMonths(i);
                LocalDateTime mStart = ym.atDay(1).atStartOfDay();
                LocalDateTime mEnd = ym.atEndOfMonth().atTime(LocalTime.MAX);

                BigDecimal mIn = transactionRepository.getIncomeForPeriod(accNo, mStart, mEnd);
                BigDecimal mOut = transactionRepository.getExpensesForPeriod(accNo, mStart, mEnd);

                totalInflow = totalInflow.add(mIn);
                totalOutflow = totalOutflow.add(mOut);
                monthlyInflows.add(mIn);
                monthlyOutflows.add(mOut);
            }

            BigDecimal avgInflow = analysisMonths > 0
                    ? totalInflow.divide(new BigDecimal(analysisMonths), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            BigDecimal avgOutflow = analysisMonths > 0
                    ? totalOutflow.divide(new BigDecimal(analysisMonths), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            // Generate forecast
            List<CashFlowResponse.MonthForecast> forecast = new ArrayList<>();
            BigDecimal runningBalance = wallet.getBalance();
            BigDecimal baseConfidence = new BigDecimal("0.85");
            BigDecimal confidenceDecay = new BigDecimal("0.10");

            for (int i = 1; i <= forecastMonths; i++) {
                YearMonth futureMonth = now.plusMonths(i);

                // Apply slight growth/decay trends based on recent data
                BigDecimal trendFactor = BigDecimal.ONE;
                if (monthlyInflows.size() >= 2) {
                    BigDecimal recent = monthlyInflows.get(monthlyInflows.size() - 1);
                    BigDecimal previous = monthlyInflows.get(monthlyInflows.size() - 2);
                    if (previous.compareTo(BigDecimal.ZERO) > 0) {
                        trendFactor = recent.divide(previous, 4, RoundingMode.HALF_UP);
                        // Cap trend between 0.9 and 1.1
                        trendFactor = trendFactor.max(new BigDecimal("0.9")).min(new BigDecimal("1.1"));
                    }
                }

                BigDecimal projIncome = avgInflow.multiply(trendFactor).setScale(2, RoundingMode.HALF_UP);
                BigDecimal projExpenses = avgOutflow.setScale(2, RoundingMode.HALF_UP);
                runningBalance = runningBalance.add(projIncome).subtract(projExpenses);

                BigDecimal confidence = baseConfidence.subtract(
                        confidenceDecay.multiply(new BigDecimal(i - 1)));
                confidence = confidence.max(new BigDecimal("0.40"));

                forecast.add(CashFlowResponse.MonthForecast.builder()
                        .month(futureMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")))
                        .projectedIncome(projIncome)
                        .projectedExpenses(projExpenses)
                        .projectedBalance(runningBalance)
                        .confidence(confidence)
                        .build());
            }

            CashFlowResponse response = CashFlowResponse.builder()
                    .currentBalance(wallet.getBalance())
                    .projectedBalance(runningBalance)
                    .averageMonthlyInflow(avgInflow)
                    .averageMonthlyOutflow(avgOutflow)
                    .forecast(forecast)
                    .build();

            return new ApiResponse<>(200, "Cash flow forecast retrieved", response);

        } catch (Exception e) {
            log.error("Failed to get cash flow forecast: {}", e.getMessage(), e);
            return new ApiResponse<>(500, "Unable to generate cash flow forecast.", null);
        }
    }

    // ==================== HELPERS ====================

    private BigDecimal calcPercentChange(BigDecimal previous, BigDecimal current) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return current.subtract(previous)
                .multiply(new BigDecimal("100"))
                .divide(previous, 1, RoundingMode.HALF_UP);
    }
}
