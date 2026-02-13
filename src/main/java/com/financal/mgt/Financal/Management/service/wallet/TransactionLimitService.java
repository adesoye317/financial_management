package com.financal.mgt.Financal.Management.service.wallet;

import com.financal.mgt.Financal.Management.exception.InvalidTransactionException;

import com.financal.mgt.Financal.Management.model.wallet.TransactionLimit;
import com.financal.mgt.Financal.Management.repository.wallet.TransactionLimitRepository;
import com.financal.mgt.Financal.Management.repository.wallet.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionLimitService {

    private final TransactionLimitRepository limitRepository;
    private final WalletTransactionRepository transactionRepository;

    // Cache so we don't hit DB on every transaction
    @Cacheable(value = "transactionLimits", key = "#limitKey")
    public BigDecimal getLimit(String limitKey, BigDecimal fallback) {
        return limitRepository.findByLimitKeyAndActiveTrue(limitKey)
                .map(TransactionLimit::getLimitValue)
                .orElse(fallback);
    }

    public void validateTransaction(BigDecimal amount, String walletAccountNumber) {
        BigDecimal minAmount = getLimit("MIN_TRANSACTION_AMOUNT", new BigDecimal("1.00"));
        BigDecimal maxSingle = getLimit("MAX_SINGLE_TRANSACTION", new BigDecimal("5000000"));
        BigDecimal dailyLimit = getLimit("DAILY_TRANSACTION_LIMIT", new BigDecimal("10000000"));

        if (amount.compareTo(minAmount) < 0) {
            throw new InvalidTransactionException(
                    "Amount below minimum of ₦" + minAmount.toPlainString()
            );
        }

        if (amount.compareTo(maxSingle) > 0) {
            throw new InvalidTransactionException(
                    "Amount exceeds single transaction limit of ₦" + maxSingle.toPlainString()
            );
        }

        // Check daily total
        BigDecimal dailyTotal = transactionRepository.getDailyTotal(
                walletAccountNumber,
                LocalDateTime.now().with(LocalTime.MIN),
                LocalDateTime.now()
        );

        if (dailyTotal.add(amount).compareTo(dailyLimit) > 0) {
            throw new InvalidTransactionException(
                    "Transaction would exceed daily limit of ₦" + dailyLimit.toPlainString()
                            + ". Today's total: ₦" + dailyTotal.toPlainString()
            );
        }
    }
}
