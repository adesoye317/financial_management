package com.financal.mgt.Financal.Management.service.account;

import com.financal.mgt.Financal.Management.dto.request.account.AddBankAccountRequest;
import com.financal.mgt.Financal.Management.dto.request.account.UpdateBankAccountRequest;
import com.financal.mgt.Financal.Management.dto.response.account.BankAccountResponse;
import com.financal.mgt.Financal.Management.dto.response.auth.ApiResponse;
import com.financal.mgt.Financal.Management.model.account.BankAccount;
import com.financal.mgt.Financal.Management.repository.account.BankAccountRepository;
import com.financal.mgt.Financal.Management.service.audit.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BankAccountServiceImpl implements BankAccountService {

    private final BankAccountRepository bankAccountRepository;
    private final AuditService auditService;

    private static final int MAX_BANK_ACCOUNTS = 5;

    @Override
    @Transactional
    public ApiResponse<BankAccountResponse> addBankAccount(String userId, AddBankAccountRequest request) {
        try {
            if (bankAccountRepository.existsByBankAccountNumberAndUserId(
                    request.getBankAccountNumber(), userId)) {
                return new ApiResponse<>(400, "This bank account is already linked to your profile", null);
            }

            List<BankAccount> existing = bankAccountRepository.findByUserIdOrderByCreatedAtDesc(userId);
            if (existing.size() >= MAX_BANK_ACCOUNTS) {
                return new ApiResponse<>(400, "Maximum of " + MAX_BANK_ACCOUNTS + " bank accounts allowed", null);
            }

            BankAccount bankAccount = BankAccount.builder()
                    .bankAccountNumber(request.getBankAccountNumber())
                    .bankName(request.getBankName())
                    .accountHolderName(request.getAccountHolderName())
                    .bankCode(request.getBankCode())
                    .accountType(request.getAccountType())
                    .userId(userId)
                    .accountStatus("ACTIVE")
                    .build();

            bankAccount = bankAccountRepository.save(bankAccount);

            auditService.log(userId, "ADD_BANK_ACCOUNT",
                    "Added bank account " + maskAccountNumber(request.getBankAccountNumber())
                            + " at " + request.getBankName(), null);

            log.info("Bank account added: userId={}, bank={}", userId, request.getBankName());

            return new ApiResponse<>(200, "Bank account added successfully", mapToResponse(bankAccount));

        } catch (Exception e) {
            log.error("Failed to add bank account: {}", e.getMessage(), e);
            return new ApiResponse<>(500, "Unable to add bank account. Please try again later.", null);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<BankAccountResponse>> getUserBankAccounts(String userId) {
        try {
            List<BankAccountResponse> accounts = bankAccountRepository
                    .findByUserIdOrderByCreatedAtDesc(userId)
                    .stream()
                    .map(this::mapToResponse)
                    .toList();

            return new ApiResponse<>(200, "Bank accounts retrieved successfully", accounts);

        } catch (Exception e) {
            log.error("Failed to get bank accounts: {}", e.getMessage(), e);
            return new ApiResponse<>(500, "Unable to retrieve bank accounts. Please try again later.", null);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<BankAccountResponse> getBankAccount(String userId, String bankId) {
        try {
            BankAccount account = bankAccountRepository.findByBankIdAndUserId(bankId, userId)
                    .orElse(null);

            if (account == null) {
                return new ApiResponse<>(404, "Bank account not found", null);
            }

            return new ApiResponse<>(200, "Bank account retrieved successfully", mapToResponse(account));

        } catch (Exception e) {
            log.error("Failed to get bank account: {}", e.getMessage(), e);
            return new ApiResponse<>(500, "Unable to retrieve bank account. Please try again later.", null);
        }
    }

    @Override
    @Transactional
    public ApiResponse<BankAccountResponse> updateBankAccount(String userId, String bankId,
                                                              UpdateBankAccountRequest request) {
        try {
            log.info("Searching for bankId={}, userId={}", bankId, userId);

            BankAccount account = bankAccountRepository.findByBankIdAndUserId(bankId, userId)
                    .orElse(null);

            if (account == null) {
                return new ApiResponse<>(404, "Bank account not found", null);
            }

            if (!"ACTIVE".equals(account.getAccountStatus())) {
                return new ApiResponse<>(400, "Cannot update an inactive bank account", null);
            }

            if (request.getBankName() != null) account.setBankName(request.getBankName());
            if (request.getAccountHolderName() != null) account.setAccountHolderName(request.getAccountHolderName());
            if (request.getBankCode() != null) account.setBankCode(request.getBankCode());
            if (request.getAccountType() != null) account.setAccountType(request.getAccountType());

            account = bankAccountRepository.save(account);

            auditService.log(userId, "UPDATE_BANK_ACCOUNT",
                    "Updated bank account " + maskAccountNumber(account.getBankAccountNumber()), null);

            return new ApiResponse<>(200, "Bank account updated successfully", mapToResponse(account));

        } catch (Exception e) {
            log.error("Failed to update bank account: {}", e.getMessage(), e);
            return new ApiResponse<>(500, "Unable to update bank account. Please try again later.", null);
        }
    }

    @Override
    @Transactional
    public ApiResponse<Void> deactivateBankAccount(String userId, String bankId) {
        try {
            BankAccount account = bankAccountRepository.findByBankIdAndUserId(bankId, userId)
                    .orElse(null);

            if (account == null) {
                return new ApiResponse<>(404, "Bank account not found", null);
            }

            account.setAccountStatus("INACTIVE");
            bankAccountRepository.save(account);

            auditService.log(userId, "DEACTIVATE_BANK_ACCOUNT",
                    "Deactivated bank account " + maskAccountNumber(account.getBankAccountNumber()), null);

            log.info("Bank account deactivated: userId={}, bankId={}", userId, bankId);

            return new ApiResponse<>(200, "Bank account deactivated successfully", null);

        } catch (Exception e) {
            log.error("Failed to deactivate bank account: {}", e.getMessage(), e);
            return new ApiResponse<>(500, "Unable to deactivate bank account. Please try again later.", null);
        }
    }

    @Override
    @Transactional
    public ApiResponse<Void> activateBankAccount(String userId, String bankId) {
        try {
            BankAccount account = bankAccountRepository.findByBankIdAndUserId(bankId, userId)
                    .orElse(null);

            if (account == null) {
                return new ApiResponse<>(404, "Bank account not found", null);
            }

            account.setAccountStatus("ACTIVE");
            bankAccountRepository.save(account);

            auditService.log(userId, "ACTIVATE_BANK_ACCOUNT",
                    "activated bank account " + maskAccountNumber(account.getBankAccountNumber()), null);

            log.info("Bank account activated: userId={}, bankId={}", userId, bankId);

            return new ApiResponse<>(200, "Bank account activated successfully", null);

        } catch (Exception e) {
            log.error("Failed to deactivate bank account: {}", e.getMessage(), e);
            return new ApiResponse<>(500, "Unable to activate bank account. Please try again later.", null);
        }
    }

    @Override
    @Transactional
    public ApiResponse<Void> deleteBankAccount(String userId, String bankId) {
        try {
            BankAccount account = bankAccountRepository.findByBankIdAndUserId(bankId, userId)
                    .orElse(null);

            if (account == null) {
                return new ApiResponse<>(404, "Bank account not found", null);
            }

            bankAccountRepository.delete(account);

            auditService.log(userId, "DELETE_BANK_ACCOUNT",
                    "Deleted bank account " + maskAccountNumber(account.getBankAccountNumber())
                            + " at " + account.getBankName(), null);

            log.info("Bank account deleted: userId={}, bankId={}", userId, bankId);

            return new ApiResponse<>(200, "Bank account deleted successfully", null);

        } catch (Exception e) {
            log.error("Failed to delete bank account: {}", e.getMessage(), e);
            return new ApiResponse<>(500, "Unable to delete bank account. Please try again later.", null);
        }
    }

    // ==================== HELPERS ====================

    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) return "****";
        return "******" + accountNumber.substring(accountNumber.length() - 4);
    }

    private BankAccountResponse mapToResponse(BankAccount account) {
        return BankAccountResponse.builder()
                .bankId(account.getBankId())
                .bankAccountNumber(account.getBankAccountNumber())
                .bankName(account.getBankName())
                .accountHolderName(account.getAccountHolderName())
                .accountStatus(account.getAccountStatus())
                .bankCode(account.getBankCode())
                .accountType(account.getAccountType())
                .createdAt(account.getCreatedAt())
                .build();
    }
}