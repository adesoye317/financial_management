package com.financal.mgt.Financal.Management.service.wallet;

import com.financal.mgt.Financal.Management.dto.request.wallet.FundWalletRequest;
import com.financal.mgt.Financal.Management.dto.request.wallet.TransferRequest;
import com.financal.mgt.Financal.Management.dto.request.wallet.WithdrawRequest;
import com.financal.mgt.Financal.Management.dto.response.auth.ApiResponse;
import com.financal.mgt.Financal.Management.dto.response.wallet.TransactionResponse;
import com.financal.mgt.Financal.Management.dto.response.wallet.WalletResponse;
import com.financal.mgt.Financal.Management.enums.wallet.TransactionStatus;
import com.financal.mgt.Financal.Management.enums.wallet.TransactionType;
import com.financal.mgt.Financal.Management.model.wallet.Wallet;
import com.financal.mgt.Financal.Management.model.wallet.WalletTransaction;
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
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final TransactionLimitService limitService;
    private final AuditService auditService;

    @Override
    @Transactional
    public ApiResponse<WalletResponse> createWallet(String userId, String accountHolderName) {
        try {
            if (walletRepository.existsByUserId(userId)) {
                return new ApiResponse<>(400, "Wallet already exists for this user", null);
            }

            Wallet wallet = Wallet.builder()
                    .userId(userId)
                    .accountHolderName(accountHolderName)
                    .accountNumber(generateAccountNumber())
                    .balance(BigDecimal.ZERO)
                    .currency("NGN")
                    .build();

            wallet = walletRepository.save(wallet);

            auditService.log(userId, "CREATE_WALLET",
                    "Wallet created with account number " + wallet.getAccountNumber(), null);

            log.info("Wallet created for user: {}", userId);

            return new ApiResponse<>(200, "Wallet created successfully", mapToResponse(wallet));

        } catch (Exception e) {
            log.error("Failed to create wallet: {}", e.getMessage(), e);
            return new ApiResponse<>(500, "Unable to create wallet. Please try again later.", null);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<WalletResponse> getWallet(String userId) {
        try {
            Wallet wallet = walletRepository.findByUserId(userId).orElse(null);

            if (wallet == null) {
                return new ApiResponse<>(404, "Wallet not found", null);
            }

            return new ApiResponse<>(200, "Wallet retrieved successfully", mapToResponse(wallet));

        } catch (Exception e) {
            log.error("Failed to get wallet: {}", e.getMessage(), e);
            return new ApiResponse<>(500, "Unable to retrieve wallet. Please try again later.", null);
        }
    }

    @Override
    @Transactional
    public ApiResponse<WalletResponse> fundWallet(String userId, FundWalletRequest request) {
        try {
            Wallet wallet = walletRepository.findByUserId(userId).orElse(null);

            if (wallet == null) {
                return new ApiResponse<>(404, "Wallet not found", null);
            }

            // Validate transaction limits
            ApiResponse<Void> limitCheck = validateLimits(request.getAmount(), wallet.getAccountNumber());
            if (limitCheck != null) return new ApiResponse<>(limitCheck.getStatusCode(), limitCheck.getMessage(), null);

            Wallet lockedWallet = walletRepository
                    .findByAccountNumberForUpdate(wallet.getAccountNumber()).orElse(null);

            if (lockedWallet == null) {
                return new ApiResponse<>(404, "Wallet not found", null);
            }

            if (!lockedWallet.isActive()) {
                return new ApiResponse<>(400, "Wallet is inactive", null);
            }

            lockedWallet.setBalance(lockedWallet.getBalance().add(request.getAmount()));
            walletRepository.save(lockedWallet);

            String description = request.getDescription() != null
                    ? request.getDescription()
                    : "Wallet Funding From Bank Transfer";

            saveTransaction(
                    lockedWallet.getAccountNumber(),
                    request.getAmount(),
                    TransactionType.FUNDING,
                    description,
                    null,
                    lockedWallet.getAccountNumber()
            );

            auditService.log(userId, "FUND_WALLET",
                    "Funded ₦" + request.getAmount() + " to wallet " + lockedWallet.getAccountNumber(),
                    null);

            log.info("Wallet funded: userId={}, amount={}, account={}",
                    userId, request.getAmount(), lockedWallet.getAccountNumber());

            return new ApiResponse<>(200, "Wallet funded successfully", mapToResponse(lockedWallet));

        } catch (Exception e) {
            log.error("Failed to fund wallet: {}", e.getMessage(), e);
            return new ApiResponse<>(500, "Unable to fund wallet. Please try again later.", null);
        }
    }

    @Override
    @Transactional
    public ApiResponse<WalletResponse> transfer(String userId, TransferRequest request) {
        try {
            Wallet senderWallet = walletRepository.findByUserId(userId).orElse(null);

            if (senderWallet == null) {
                return new ApiResponse<>(404, "Wallet not found", null);
            }

            if (senderWallet.getAccountNumber().equals(request.getReceiverAccountNumber())) {
                return new ApiResponse<>(400, "Cannot transfer to your own wallet", null);
            }

            // Validate transaction limits
            ApiResponse<Void> limitCheck = validateLimits(request.getAmount(), senderWallet.getAccountNumber());
            if (limitCheck != null) return new ApiResponse<>(limitCheck.getStatusCode(), limitCheck.getMessage(), null);

            // Lock in consistent order (lower account number first) to prevent deadlocks
            String senderAcc = senderWallet.getAccountNumber();
            String receiverAcc = request.getReceiverAccountNumber();

            Wallet firstLock, secondLock;
            if (senderAcc.compareTo(receiverAcc) < 0) {
                firstLock = walletRepository.findByAccountNumberForUpdate(senderAcc).orElse(null);
                secondLock = walletRepository.findByAccountNumberForUpdate(receiverAcc).orElse(null);
            } else {
                secondLock = walletRepository.findByAccountNumberForUpdate(receiverAcc).orElse(null);
                firstLock = walletRepository.findByAccountNumberForUpdate(senderAcc).orElse(null);
            }

            if (firstLock == null || secondLock == null) {
                return new ApiResponse<>(404, "One or both wallets not found", null);
            }

            Wallet lockedSender = senderAcc.equals(firstLock.getAccountNumber()) ? firstLock : secondLock;
            Wallet lockedReceiver = receiverAcc.equals(firstLock.getAccountNumber()) ? firstLock : secondLock;

            if (!lockedSender.isActive()) {
                return new ApiResponse<>(400, "Your wallet is inactive", null);
            }
            if (!lockedReceiver.isActive()) {
                return new ApiResponse<>(400, "Receiver wallet is inactive", null);
            }

            if (lockedSender.getBalance().compareTo(request.getAmount()) < 0) {
                return new ApiResponse<>(400, "Insufficient balance", null);
            }

            // Debit sender
            lockedSender.setBalance(lockedSender.getBalance().subtract(request.getAmount()));
            walletRepository.save(lockedSender);

            // Credit receiver
            lockedReceiver.setBalance(lockedReceiver.getBalance().add(request.getAmount()));
            walletRepository.save(lockedReceiver);

            String desc = request.getDescription() != null
                    ? request.getDescription()
                    : "Transfer to " + lockedReceiver.getAccountHolderName();

            // Sender transaction record
            saveTransaction(
                    lockedSender.getAccountNumber(),
                    request.getAmount(),
                    TransactionType.TRANSFER,
                    desc,
                    lockedSender.getAccountNumber(),
                    lockedReceiver.getAccountNumber()
            );

            // Receiver transaction record
            saveTransaction(
                    lockedReceiver.getAccountNumber(),
                    request.getAmount(),
                    TransactionType.FUNDING,
                    "Transfer from " + lockedSender.getAccountHolderName(),
                    lockedSender.getAccountNumber(),
                    lockedReceiver.getAccountNumber()
            );

            auditService.log(userId, "TRANSFER",
                    "Transferred ₦" + request.getAmount()
                            + " from " + lockedSender.getAccountNumber()
                            + " to " + lockedReceiver.getAccountNumber(),
                    null);

            log.info("Transfer completed: from={}, to={}, amount={}",
                    lockedSender.getAccountNumber(), lockedReceiver.getAccountNumber(), request.getAmount());

            return new ApiResponse<>(200, "Transfer successful", mapToResponse(lockedSender));

        } catch (Exception e) {
            log.error("Failed to transfer: {}", e.getMessage(), e);
            return new ApiResponse<>(500, "Unable to complete transfer. Please try again later.", null);
        }
    }

    @Override
    @Transactional
    public ApiResponse<WalletResponse> withdraw(String userId, WithdrawRequest request) {
        try {
            Wallet wallet = walletRepository.findByUserId(userId).orElse(null);

            if (wallet == null) {
                return new ApiResponse<>(404, "Wallet not found", null);
            }

            // Validate transaction limits
            ApiResponse<Void> limitCheck = validateLimits(request.getAmount(), wallet.getAccountNumber());
            if (limitCheck != null) return new ApiResponse<>(limitCheck.getStatusCode(), limitCheck.getMessage(), null);

            Wallet lockedWallet = walletRepository
                    .findByAccountNumberForUpdate(wallet.getAccountNumber()).orElse(null);

            if (lockedWallet == null) {
                return new ApiResponse<>(404, "Wallet not found", null);
            }

            if (!lockedWallet.isActive()) {
                return new ApiResponse<>(400, "Wallet is inactive", null);
            }

            if (lockedWallet.getBalance().compareTo(request.getAmount()) < 0) {
                return new ApiResponse<>(400, "Insufficient balance", null);
            }

            lockedWallet.setBalance(lockedWallet.getBalance().subtract(request.getAmount()));
            walletRepository.save(lockedWallet);

            String description = request.getDescription() != null
                    ? request.getDescription()
                    : "Withdrawal to bank account";

            saveTransaction(
                    lockedWallet.getAccountNumber(),
                    request.getAmount(),
                    TransactionType.WITHDRAWAL,
                    description,
                    lockedWallet.getAccountNumber(),
                    null
            );

            auditService.log(userId, "WITHDRAW",
                    "Withdrew ₦" + request.getAmount() + " from wallet " + lockedWallet.getAccountNumber(),
                    null);

            log.info("Withdrawal completed: userId={}, amount={}, account={}",
                    userId, request.getAmount(), lockedWallet.getAccountNumber());

            return new ApiResponse<>(200, "Withdrawal successful", mapToResponse(lockedWallet));

        } catch (Exception e) {
            log.error("Failed to withdraw: {}", e.getMessage(), e);
            return new ApiResponse<>(500, "Unable to complete withdrawal. Please try again later.", null);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<Page<TransactionResponse>> getTransactions(String userId, Pageable pageable) {
        try {
            Wallet wallet = walletRepository.findByUserId(userId).orElse(null);

            if (wallet == null) {
                return new ApiResponse<>(404, "Wallet not found", null);
            }

            Page<TransactionResponse> transactions = transactionRepository
                    .findByWalletAccountNumberOrderByCreatedAtDesc(
                            wallet.getAccountNumber(), pageable)
                    .map(this::mapToTransactionResponse);

            return new ApiResponse<>(200, "Transactions retrieved successfully", transactions);

        } catch (Exception e) {
            log.error("Failed to get transactions: {}", e.getMessage(), e);
            return new ApiResponse<>(500, "Unable to retrieve transactions. Please try again later.", null);
        }
    }

    // ==================== HELPERS ====================

    private ApiResponse<Void> validateLimits(BigDecimal amount, String accountNumber) {
        try {
            limitService.validateTransaction(amount, accountNumber);
            return null; // null means validation passed
        } catch (Exception e) {
            return new ApiResponse<>(400, e.getMessage(), null);
        }
    }

    private void saveTransaction(String walletAccNo, BigDecimal amount,
                                 TransactionType type, String description,
                                 String senderAccNo, String receiverAccNo) {
        WalletTransaction txn = WalletTransaction.builder()
                .walletAccountNumber(walletAccNo)
                .amount(amount)
                .type(type)
                .status(TransactionStatus.SUCCESS)
                .description(description)
                .senderAccountNumber(senderAccNo)
                .receiverAccountNumber(receiverAccNo)
                .build();
        transactionRepository.save(txn);
    }

    private String generateAccountNumber() {
        String accountNumber;
        int attempts = 0;
        int maxAttempts = 10;

        do {
            if (attempts >= maxAttempts) {
                throw new RuntimeException("Failed to generate unique account number after " + maxAttempts + " attempts");
            }
            long number = ThreadLocalRandom.current().nextLong(1_000_000_000L, 9_999_999_999L);
            accountNumber = String.valueOf(number);
            attempts++;
        } while (walletRepository.findByAccountNumber(accountNumber).isPresent());

        return accountNumber;
    }

    private WalletResponse mapToResponse(Wallet wallet) {
        return WalletResponse.builder()
                .accountNumber(wallet.getAccountNumber())
                .balance(wallet.getBalance())
                .currency(wallet.getCurrency())
                .accountHolderName(wallet.getAccountHolderName())
                .build();
    }

    private TransactionResponse mapToTransactionResponse(WalletTransaction txn) {
        return TransactionResponse.builder()
                .transactionRef(txn.getTransactionRef())
                .amount(txn.getAmount())
                .type(txn.getType().name())
                .status(txn.getStatus().name())
                .description(txn.getDescription())
                .createdAt(txn.getCreatedAt())
                .build();
    }
}