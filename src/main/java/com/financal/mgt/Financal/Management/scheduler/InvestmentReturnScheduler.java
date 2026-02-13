package com.financal.mgt.Financal.Management.scheduler;


import com.financal.mgt.Financal.Management.enums.investment.InvestmentStatus;
import com.financal.mgt.Financal.Management.enums.investment.InvestmentTxnType;
import com.financal.mgt.Financal.Management.model.investment.InvestmentTransaction;
import com.financal.mgt.Financal.Management.model.investment.UserInvestment;
import com.financal.mgt.Financal.Management.repository.investment.InvestmentTransactionRepository;
import com.financal.mgt.Financal.Management.repository.investment.UserInvestmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvestmentReturnScheduler {

    private final UserInvestmentRepository investmentRepository;
    private final InvestmentTransactionRepository txnRepository;

    /**
     * Runs daily at midnight — calculates daily returns for all active investments
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void calculateDailyReturns() {
        log.info("Starting daily investment return calculation...");

        List<UserInvestment> activeInvestments = investmentRepository.findByStatus(InvestmentStatus.ACTIVE);
        int processed = 0;

        for (UserInvestment inv : activeInvestments) {
            try {
                // Daily return = (annualReturnPct / 100) / 365 * amountInvested
                BigDecimal dailyRate = inv.getAnnualReturnPct()
                        .divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP)
                        .divide(new BigDecimal("365"), 10, RoundingMode.HALF_UP);

                BigDecimal dailyReturn = inv.getAmountInvested()
                        .multiply(dailyRate)
                        .setScale(2, RoundingMode.HALF_UP);

                inv.setReturnsEarned(inv.getReturnsEarned().add(dailyReturn));
                inv.setCurrentValue(inv.getAmountInvested().add(inv.getReturnsEarned()));

                // Check maturity
                if (!LocalDate.now().isBefore(inv.getMaturityDate())) {
                    inv.setStatus(InvestmentStatus.MATURED);
                    log.info("Investment matured: ref={}, returns={}",
                            inv.getInvestmentRef(), inv.getReturnsEarned());
                }

                investmentRepository.save(inv);
                processed++;

            } catch (Exception e) {
                log.error("Failed to process return for investment {}: {}",
                        inv.getInvestmentRef(), e.getMessage());
            }
        }

        log.info("Daily return calculation complete. Processed {} investments.", processed);
    }

    /**
     * Runs on 1st of every month — records monthly return transactions
     */
    @Scheduled(cron = "0 0 1 1 * *")
    @Transactional
    public void recordMonthlyReturnTransactions() {
        log.info("Recording monthly return transactions...");

        List<UserInvestment> activeInvestments = investmentRepository.findByStatus(InvestmentStatus.ACTIVE);
        YearMonth lastMonth = YearMonth.now().minusMonths(1);

        for (UserInvestment inv : activeInvestments) {
            try {
                BigDecimal monthlyReturn = inv.getAnnualReturnPct()
                        .divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP)
                        .divide(new BigDecimal("12"), 10, RoundingMode.HALF_UP)
                        .multiply(inv.getAmountInvested())
                        .setScale(2, RoundingMode.HALF_UP);

                InvestmentTransaction txn = InvestmentTransaction.builder()
                        .userInvestmentId(inv.getId())
                        .userId(inv.getUserId())
                        .amount(monthlyReturn)
                        .type(InvestmentTxnType.RETURN)
                        .description("Monthly return - " + lastMonth.format(
                                java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy")))
                        .build();
                txnRepository.save(txn);

            } catch (Exception e) {
                log.error("Failed to record monthly txn for {}: {}",
                        inv.getInvestmentRef(), e.getMessage());
            }
        }

        log.info("Monthly return transactions recorded.");
    }
}

