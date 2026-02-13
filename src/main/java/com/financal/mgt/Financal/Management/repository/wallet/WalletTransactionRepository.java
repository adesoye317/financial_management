package com.financal.mgt.Financal.Management.repository.wallet;

import com.financal.mgt.Financal.Management.model.wallet.WalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    Page<WalletTransaction> findByWalletAccountNumberOrderByCreatedAtDesc(
            String walletAccountNumber, Pageable pageable
    );


    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM WalletTransaction t " +
            "WHERE t.walletAccountNumber = :accountNumber " +
            "AND t.status = 'SUCCESS' " +
            "AND t.createdAt BETWEEN :startOfDay AND :now")
    BigDecimal getDailyTotal(
            @Param("accountNumber") String accountNumber,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("now") LocalDateTime now);


    // Monthly income (credits)
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM WalletTransaction t " +
            "WHERE t.walletAccountNumber = :accNo " +
            "AND t.type IN ('FUNDING', 'INVESTMENT_CREDIT') " +
            "AND t.status = 'SUCCESS' " +
            "AND t.createdAt BETWEEN :start AND :end")
    BigDecimal getIncomeForPeriod(
            @Param("accNo") String accNo,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    // Monthly expenses (debits)
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM WalletTransaction t " +
            "WHERE t.walletAccountNumber = :accNo " +
            "AND t.type IN ('TRANSFER', 'WITHDRAWAL', 'INVESTMENT_DEBIT') " +
            "AND t.status = 'SUCCESS' " +
            "AND t.createdAt BETWEEN :start AND :end")
    BigDecimal getExpensesForPeriod(
            @Param("accNo") String accNo,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    // Transactions by type for a period
    @Query("SELECT t.type, COALESCE(SUM(t.amount), 0) FROM WalletTransaction t " +
            "WHERE t.walletAccountNumber = :accNo " +
            "AND t.status = 'SUCCESS' " +
            "AND t.createdAt BETWEEN :start AND :end " +
            "GROUP BY t.type")
    List<Object[]> getAmountGroupedByType(
            @Param("accNo") String accNo,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    // All successful transactions for a period
    @Query("SELECT t FROM WalletTransaction t " +
            "WHERE t.walletAccountNumber = :accNo " +
            "AND t.status = 'SUCCESS' " +
            "AND t.createdAt BETWEEN :start AND :end " +
            "ORDER BY t.createdAt ASC")
    List<WalletTransaction> findSuccessfulForPeriod(
            @Param("accNo") String accNo,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
