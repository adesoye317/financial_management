package com.financal.mgt.Financal.Management.service.investment;


import com.financal.mgt.Financal.Management.dto.request.investment.*;
import com.financal.mgt.Financal.Management.dto.response.auth.ApiResponse;
import com.financal.mgt.Financal.Management.dto.response.investment.*;
import com.financal.mgt.Financal.Management.enums.investment.*;
import com.financal.mgt.Financal.Management.enums.wallet.TransactionStatus;
import com.financal.mgt.Financal.Management.enums.wallet.TransactionType;
import com.financal.mgt.Financal.Management.model.investment.*;
import com.financal.mgt.Financal.Management.model.wallet.Wallet;
import com.financal.mgt.Financal.Management.model.wallet.WalletTransaction;
import com.financal.mgt.Financal.Management.repository.investment.*;
import com.financal.mgt.Financal.Management.repository.wallet.WalletRepository;
import com.financal.mgt.Financal.Management.repository.wallet.WalletTransactionRepository;
import com.financal.mgt.Financal.Management.service.audit.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvestmentServiceImpl implements InvestmentService {

    private final InvestmentPoolRepository poolRepository;
    private final UserInvestmentRepository investmentRepository;
    private final InvestmentTransactionRepository investmentTxnRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTxnRepository;
    private final AuditService auditService;

    // ==================== POOLS ====================

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<Page<PoolResponse>> getPools(String riskLevel, String status, Pageable pageable) {
        try {
            PoolStatus poolStatus = status != null ? PoolStatus.valueOf(status) : PoolStatus.OPEN;

            Page<InvestmentPool> pools;
            if (riskLevel != null) {
                RiskLevel risk = RiskLevel.valueOf(riskLevel);
                pools = poolRepository.findByStatusAndRiskLevel(poolStatus, risk, pageable);
            } else {
                pools = poolRepository.findByStatus(poolStatus, pageable);
            }

            Page<PoolResponse> response = pools.map(this::mapPoolResponse);
            return new ApiResponse<>(200, "Investment pools retrieved successfully", response);

        } catch (IllegalArgumentException e) {
            return new ApiResponse<>(400, "Invalid filter value: " + e.getMessage(), null);
        } catch (Exception e) {
            log.error("Failed to get pools: {}", e.getMessage(), e);
            return new ApiResponse<>(500, "Unable to retrieve investment pools.", null);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<PoolResponse> getPool(Long poolId) {
        try {
            InvestmentPool pool = poolRepository.findById(poolId).orElse(null);
            if (pool == null) {
                return new ApiResponse<>(404, "Investment pool not found", null);
            }
            return new ApiResponse<>(200, "Pool retrieved successfully", mapPoolResponse(pool));
        } catch (Exception e) {
            log.error("Failed to get pool: {}", e.getMessage(), e);
            return new ApiResponse<>(500, "Unable to retrieve pool.", null);
        }
    }

    // ==================== INVEST ====================

    @Override
    @Transactional
    public ApiResponse<InvestResultResponse> invest(String userId, InvestRequest request) {
        try {
            // 1. Validate pool
            InvestmentPool pool = poolRepository.findByIdForUpdate(request.getPoolId()).orElse(null);
            if (pool == null) {
                return new ApiResponse<>(404, "Investment pool not found", null);
            }

            if (pool.getStatus() != PoolStatus.OPEN) {
                return new ApiResponse<>(400, "This pool is not accepting investments", null);
            }

            BigDecimal amount = request.getAmount();

            if (amount.compareTo(pool.getMinInvestment()) < 0) {
                return new ApiResponse<>(400,
                        "Minimum investment for this pool is ₦" + pool.getMinInvestment().toPlainString(), null);
            }

            if (amount.compareTo(pool.getMaxInvestment()) > 0) {
                return new ApiResponse<>(400,
                        "Maximum investment for this pool is ₦" + pool.getMaxInvestment().toPlainString(), null);
            }

            if (amount.compareTo(pool.getAvailableSlots()) > 0) {
                return new ApiResponse<>(400,
                        "Pool only has ₦" + pool.getAvailableSlots().toPlainString() + " available", null);
            }

            // 2. Validate wallet balance
            Wallet wallet = walletRepository.findByUserId(userId).orElse(null);
            if (wallet == null) {
                return new ApiResponse<>(404, "Wallet not found", null);
            }

            Wallet lockedWallet = walletRepository
                    .findByAccountNumberForUpdate(wallet.getAccountNumber()).orElse(null);
            if (lockedWallet == null) {
                return new ApiResponse<>(404, "Wallet not found", null);
            }

            if (!lockedWallet.isActive()) {
                return new ApiResponse<>(400, "Wallet is inactive", null);
            }

            if (lockedWallet.getBalance().compareTo(amount) < 0) {
                return new ApiResponse<>(400, "Insufficient wallet balance", null);
            }

            // 3. Debit wallet
            lockedWallet.setBalance(lockedWallet.getBalance().subtract(amount));
            walletRepository.save(lockedWallet);

            // 4. Record wallet transaction
            WalletTransaction walletTxn = WalletTransaction.builder()
                    .walletAccountNumber(lockedWallet.getAccountNumber())
                    .amount(amount)
                    .type(TransactionType.INVESTMENT_DEBIT)
                    .status(TransactionStatus.SUCCESS)
                    .description("Investment in " + pool.getPoolName())
                    .senderAccountNumber(lockedWallet.getAccountNumber())
                    .build();
            walletTxnRepository.save(walletTxn);

            // 5. Update pool
            pool.setCurrentInvested(pool.getCurrentInvested().add(amount));
            if (pool.getAvailableSlots().compareTo(BigDecimal.ZERO) <= 0) {
                pool.setStatus(PoolStatus.CLOSED);
            }
            poolRepository.save(pool);

            // 6. Create user investment
            LocalDate maturityDate = LocalDate.now().plusMonths(pool.getDurationMonths());

            UserInvestment investment = UserInvestment.builder()
                    .userId(userId)
                    .poolId(pool.getId())
                    .poolName(pool.getPoolName())
                    .riskLevel(pool.getRiskLevel().name())
                    .annualReturnPct(pool.getAnnualReturnPct())
                    .amountInvested(amount)
                    .currentValue(amount)
                    .returnsEarned(BigDecimal.ZERO)
                    .status(InvestmentStatus.ACTIVE)
                    .autoReinvest(request.isAutoReinvest())
                    .maturityDate(maturityDate)
                    .build();
            investment = investmentRepository.save(investment);

            // 7. Record investment transaction
            saveInvestmentTxn(investment.getId(), userId, amount,
                    InvestmentTxnType.INVESTMENT, "Investment in " + pool.getPoolName());

            // 8. Audit
            auditService.log(userId, "INVEST",
                    "Invested ₦" + amount.toPlainString() + " in " + pool.getPoolName()
                            + " (" + pool.getRiskLevel() + " risk)", null);

            log.info("Investment created: userId={}, pool={}, amount={}, ref={}",
                    userId, pool.getPoolName(), amount, investment.getInvestmentRef());

            InvestResultResponse result = InvestResultResponse.builder()
                    .investmentRef(investment.getInvestmentRef())
                    .poolName(pool.getPoolName())
                    .amountInvested(amount)
                    .currentValue(amount)
                    .annualReturnPct(pool.getAnnualReturnPct())
                    .status(investment.getStatus().name())
                    .investedAt(investment.getInvestedAt())
                    .maturityDate(maturityDate)
                    .autoReinvest(investment.isAutoReinvest())
                    .walletBalanceAfter(lockedWallet.getBalance())
                    .build();

            return new ApiResponse<>(200, "Investment successful", result);

        } catch (Exception e) {
            log.error("Failed to invest: {}", e.getMessage(), e);
            return new ApiResponse<>(500, "Unable to complete investment. Please try again later.", null);
        }
    }

    // ==================== PORTFOLIO ====================

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<PortfolioResponse> getPortfolio(String userId) {
        try {
            List<UserInvestment> investments = investmentRepository.findByUserIdOrderByInvestedAtDesc(userId);

            BigDecimal totalInvested = BigDecimal.ZERO;
            BigDecimal totalCurrentValue = BigDecimal.ZERO;
            BigDecimal totalReturns = BigDecimal.ZERO;
            int active = 0;
            int matured = 0;

            List<UserInvestmentResponse> investmentResponses = new java.util.ArrayList<>();

            for (UserInvestment inv : investments) {
                totalInvested = totalInvested.add(inv.getAmountInvested());
                totalCurrentValue = totalCurrentValue.add(inv.getCurrentValue());
                totalReturns = totalReturns.add(inv.getReturnsEarned());

                if (inv.getStatus() == InvestmentStatus.ACTIVE) active++;
                if (inv.getStatus() == InvestmentStatus.MATURED) matured++;

                investmentResponses.add(mapInvestmentResponse(inv));
            }

            BigDecimal overallReturnPct = totalInvested.compareTo(BigDecimal.ZERO) > 0
                    ? totalReturns.multiply(new BigDecimal("100"))
                    .divide(totalInvested, 1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            PortfolioResponse portfolio = PortfolioResponse.builder()
                    .totalInvested(totalInvested)
                    .totalCurrentValue(totalCurrentValue)
                    .totalReturnsEarned(totalReturns)
                    .overallReturnPct(overallReturnPct)
                    .activeInvestments(active)
                    .maturedInvestments(matured)
                    .investments(investmentResponses)
                    .build();

            return new ApiResponse<>(200, "Portfolio retrieved successfully", portfolio);

        } catch (Exception e) {
            log.error("Failed to get portfolio: {}", e.getMessage(), e);
            return new ApiResponse<>(500, "Unable to retrieve portfolio.", null);
        }
    }

    // ==================== GET SINGLE INVESTMENT ====================

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<UserInvestmentResponse> getInvestment(String userId, String investmentRef) {
        try {
            UserInvestment inv = investmentRepository.findByInvestmentRefAndUserId(investmentRef, userId)
                    .orElse(null);
            if (inv == null) {
                return new ApiResponse<>(404, "Investment not found", null);
            }
            return new ApiResponse<>(200, "Investment retrieved successfully", mapInvestmentResponse(inv));
        } catch (Exception e) {
            log.error("Failed to get investment: {}", e.getMessage(), e);
            return new ApiResponse<>(500, "Unable to retrieve investment.", null);
        }
    }

    // ==================== WITHDRAW ====================

    @Override
    @Transactional
    public ApiResponse<WithdrawResultResponse> withdrawInvestment(String userId, String investmentRef,
                                                                  WithdrawInvestmentRequest request) {
        try {
            UserInvestment investment = investmentRepository.findByInvestmentRefAndUserId(investmentRef, userId)
                    .orElse(null);
            if (investment == null) {
                return new ApiResponse<>(404, "Investment not found", null);
            }

            if (investment.getStatus() == InvestmentStatus.WITHDRAWN
                    || investment.getStatus() == InvestmentStatus.WITHDRAWN_EARLY) {
                return new ApiResponse<>(400, "Investment already withdrawn", null);
            }

            // Determine if early withdrawal
            boolean isEarly = LocalDate.now().isBefore(investment.getMaturityDate());
            BigDecimal penalty = BigDecimal.ZERO;

            InvestmentPool pool = poolRepository.findById(investment.getPoolId()).orElse(null);

            if (isEarly && pool != null) {
                BigDecimal penaltyPct = pool.getEarlyWithdrawalPenaltyPct();
                penalty = investment.getCurrentValue().multiply(penaltyPct)
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            }

            BigDecimal totalPayout = investment.getCurrentValue().subtract(penalty);

            // Credit wallet
            Wallet wallet = walletRepository.findByUserId(userId).orElse(null);
            if (wallet == null) {
                return new ApiResponse<>(404, "Wallet not found", null);
            }

            Wallet lockedWallet = walletRepository
                    .findByAccountNumberForUpdate(wallet.getAccountNumber()).orElse(null);
            if (lockedWallet == null) {
                return new ApiResponse<>(404, "Wallet not found", null);
            }

            lockedWallet.setBalance(lockedWallet.getBalance().add(totalPayout));
            walletRepository.save(lockedWallet);

            // Record wallet transaction
            WalletTransaction walletTxn = WalletTransaction.builder()
                    .walletAccountNumber(lockedWallet.getAccountNumber())
                    .amount(totalPayout)
                    .type(TransactionType.INVESTMENT_CREDIT)
                    .status(TransactionStatus.SUCCESS)
                    .description((isEarly ? "Early withdrawal" : "Withdrawal") + " from " + investment.getPoolName())
                    .receiverAccountNumber(lockedWallet.getAccountNumber())
                    .build();
            walletTxnRepository.save(walletTxn);

            // Update pool
            if (pool != null) {
                pool.setCurrentInvested(pool.getCurrentInvested().subtract(investment.getAmountInvested()));
                if (pool.getStatus() == PoolStatus.CLOSED && pool.getAvailableSlots().compareTo(BigDecimal.ZERO) > 0) {
                    pool.setStatus(PoolStatus.OPEN);
                }
                poolRepository.save(pool);
            }

            // Update investment
            InvestmentStatus newStatus = isEarly ? InvestmentStatus.WITHDRAWN_EARLY : InvestmentStatus.WITHDRAWN;
            investment.setStatus(newStatus);
            investment.setWithdrawnAt(LocalDateTime.now());
            investmentRepository.save(investment);

            // Record investment transactions
            saveInvestmentTxn(investment.getId(), userId, totalPayout,
                    InvestmentTxnType.WITHDRAWAL,
                    (isEarly ? "Early withdrawal" : "Withdrawal") + " from " + investment.getPoolName());

            if (penalty.compareTo(BigDecimal.ZERO) > 0) {
                saveInvestmentTxn(investment.getId(), userId, penalty,
                        InvestmentTxnType.PENALTY,
                        "Early withdrawal penalty (" + pool.getEarlyWithdrawalPenaltyPct() + "%)");
            }

            // Audit
            String action = isEarly ? "INVESTMENT_EARLY_WITHDRAW" : "INVESTMENT_WITHDRAW";
            auditService.log(userId, action,
                    "Withdrew ₦" + totalPayout.toPlainString() + " from " + investment.getPoolName()
                            + (isEarly ? " (penalty: ₦" + penalty.toPlainString() + ")" : ""), null);

            String message = isEarly
                    ? "Early withdrawal processed. " + pool.getEarlyWithdrawalPenaltyPct() + "% penalty applied."
                    : "Investment withdrawn successfully";

            log.info("Investment withdrawn: userId={}, ref={}, payout={}, penalty={}",
                    userId, investmentRef, totalPayout, penalty);

            WithdrawResultResponse result = WithdrawResultResponse.builder()
                    .investmentRef(investmentRef)
                    .amountInvested(investment.getAmountInvested())
                    .returnsEarned(investment.getReturnsEarned())
                    .penalty(penalty)
                    .totalPayout(totalPayout)
                    .status(newStatus.name())
                    .walletBalanceAfter(lockedWallet.getBalance())
                    .build();

            return new ApiResponse<>(200, message, result);

        } catch (Exception e) {
            log.error("Failed to withdraw investment: {}", e.getMessage(), e);
            return new ApiResponse<>(500, "Unable to process withdrawal. Please try again later.", null);
        }
    }

    // ==================== TRANSACTIONS ====================

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<Page<InvestmentTxnResponse>> getTransactions(String userId, Pageable pageable) {
        try {
            Page<InvestmentTxnResponse> txns = investmentTxnRepository
                    .findByUserIdOrderByCreatedAtDesc(userId, pageable)
                    .map(this::mapTxnResponse);
            return new ApiResponse<>(200, "Investment transactions retrieved", txns);
        } catch (Exception e) {
            log.error("Failed to get investment transactions: {}", e.getMessage(), e);
            return new ApiResponse<>(500, "Unable to retrieve transactions.", null);
        }
    }

    // ==================== ADMIN ====================

    @Override
    @Transactional
    public ApiResponse<PoolResponse> createPool(CreatePoolRequest request) {
        try {
            LocalDate maturityDate = LocalDate.now().plusMonths(request.getDurationMonths());

            BigDecimal penaltyPct = request.getEarlyWithdrawalPenaltyPct() != null
                    ? request.getEarlyWithdrawalPenaltyPct()
                    : new BigDecimal("2.00");

            InvestmentPool pool = InvestmentPool.builder()
                    .poolName(request.getPoolName())
                    .description(request.getDescription())
                    .riskLevel(request.getRiskLevel())
                    .annualReturnPct(request.getAnnualReturnPct())
                    .minInvestment(request.getMinInvestment())
                    .maxInvestment(request.getMaxInvestment())
                    .totalPoolSize(request.getTotalPoolSize())
                    .currentInvested(BigDecimal.ZERO)
                    .durationMonths(request.getDurationMonths())
                    .status(PoolStatus.OPEN)
                    .maturityDate(maturityDate)
                    .terms(request.getTerms())
                    .earlyWithdrawalPenaltyPct(penaltyPct)
                    .build();

            pool = poolRepository.save(pool);

            auditService.log(null, "CREATE_POOL",
                    "Created pool '" + pool.getPoolName() + "' with "
                            + pool.getAnnualReturnPct() + "% annual return", null);

            log.info("Investment pool created: {}", pool.getPoolName());

            return new ApiResponse<>(200, "Investment pool created successfully", mapPoolResponse(pool));

        } catch (Exception e) {
            log.error("Failed to create pool: {}", e.getMessage(), e);
            return new ApiResponse<>(500, "Unable to create pool.", null);
        }
    }

    @Override
    @Transactional
    public ApiResponse<PoolResponse> updatePoolStatus(Long poolId, String status) {
        try {
            InvestmentPool pool = poolRepository.findById(poolId).orElse(null);
            if (pool == null) {
                return new ApiResponse<>(404, "Pool not found", null);
            }

            PoolStatus newStatus = PoolStatus.valueOf(status);
            pool.setStatus(newStatus);
            poolRepository.save(pool);

            auditService.log(null, "UPDATE_POOL_STATUS",
                    "Updated pool '" + pool.getPoolName() + "' status to " + newStatus, null);

            return new ApiResponse<>(200, "Pool status updated", mapPoolResponse(pool));

        } catch (IllegalArgumentException e) {
            return new ApiResponse<>(400, "Invalid status: " + status, null);
        } catch (Exception e) {
            log.error("Failed to update pool status: {}", e.getMessage(), e);
            return new ApiResponse<>(500, "Unable to update pool status.", null);
        }
    }

    // ==================== HELPERS ====================

    private void saveInvestmentTxn(Long investmentId, String userId, BigDecimal amount,
                                   InvestmentTxnType type, String description) {
        InvestmentTransaction txn = InvestmentTransaction.builder()
                .userInvestmentId(investmentId)
                .userId(userId)
                .amount(amount)
                .type(type)
                .description(description)
                .build();
        investmentTxnRepository.save(txn);
    }

    private PoolResponse mapPoolResponse(InvestmentPool pool) {
        return PoolResponse.builder()
                .id(pool.getId())
                .poolName(pool.getPoolName())
                .description(pool.getDescription())
                .riskLevel(pool.getRiskLevel().name())
                .annualReturnPct(pool.getAnnualReturnPct())
                .minInvestment(pool.getMinInvestment())
                .maxInvestment(pool.getMaxInvestment())
                .totalPoolSize(pool.getTotalPoolSize())
                .currentInvested(pool.getCurrentInvested())
                .availableSlots(pool.getAvailableSlots())
                .durationMonths(pool.getDurationMonths())
                .status(pool.getStatus().name())
                .maturityDate(pool.getMaturityDate())
                .terms(pool.getTerms())
                .earlyWithdrawalPenaltyPct(pool.getEarlyWithdrawalPenaltyPct())
                .createdAt(pool.getCreatedAt())
                .build();
    }

    private UserInvestmentResponse mapInvestmentResponse(UserInvestment inv) {
        long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), inv.getMaturityDate());
        if (daysRemaining < 0) daysRemaining = 0;

        BigDecimal returnPct = inv.getAmountInvested().compareTo(BigDecimal.ZERO) > 0
                ? inv.getReturnsEarned().multiply(new BigDecimal("100"))
                .divide(inv.getAmountInvested(), 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return UserInvestmentResponse.builder()
                .investmentRef(inv.getInvestmentRef())
                .poolName(inv.getPoolName())
                .riskLevel(inv.getRiskLevel())
                .amountInvested(inv.getAmountInvested())
                .currentValue(inv.getCurrentValue())
                .returnsEarned(inv.getReturnsEarned())
                .returnPct(returnPct)
                .annualReturnPct(inv.getAnnualReturnPct())
                .status(inv.getStatus().name())
                .autoReinvest(inv.isAutoReinvest())
                .investedAt(inv.getInvestedAt())
                .maturityDate(inv.getMaturityDate())
                .daysRemaining(daysRemaining)
                .build();
    }

    private InvestmentTxnResponse mapTxnResponse(InvestmentTransaction txn) {
        return InvestmentTxnResponse.builder()
                .transactionRef(txn.getTransactionRef())
                .investmentRef(null) // Can join if needed
                .amount(txn.getAmount())
                .type(txn.getType().name())
                .description(txn.getDescription())
                .createdAt(txn.getCreatedAt())
                .build();
    }
}
