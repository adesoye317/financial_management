package com.financal.mgt.Financal.Management.controller.account;

import com.financal.mgt.Financal.Management.dto.request.account.AddBankAccountRequest;
import com.financal.mgt.Financal.Management.dto.request.account.DeactivateAccountRequest;
import com.financal.mgt.Financal.Management.dto.request.account.UpdateBankAccountRequest;
import com.financal.mgt.Financal.Management.dto.response.account.BankAccountResponse;
import com.financal.mgt.Financal.Management.dto.response.auth.ApiResponse;
import com.financal.mgt.Financal.Management.service.account.BankAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bank-accounts")
@RequiredArgsConstructor
public class BankAccountController {

    private final BankAccountService bankAccountService;

    @PostMapping
    public ResponseEntity<ApiResponse<BankAccountResponse>> addBankAccount(
            Authentication auth,
            @Valid @RequestBody AddBankAccountRequest request) {
        ApiResponse<BankAccountResponse> response = bankAccountService.addBankAccount(auth.getName(), request);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BankAccountResponse>>> getUserBankAccounts(Authentication auth) {
        ApiResponse<List<BankAccountResponse>> response = bankAccountService.getUserBankAccounts(auth.getName());
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @GetMapping("/{bankId}")
    public ResponseEntity<ApiResponse<BankAccountResponse>> getBankAccount(
            Authentication auth,
            @PathVariable String bankId) {
        ApiResponse<BankAccountResponse> response = bankAccountService.getBankAccount(auth.getName(), bankId);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @PostMapping("/update/bank-details")
    public ResponseEntity<ApiResponse<BankAccountResponse>> updateBankAccount(
            Authentication auth,
            @Valid @RequestBody UpdateBankAccountRequest request) {
        ApiResponse<BankAccountResponse> response = bankAccountService.updateBankAccount(
                auth.getName(), request.getBankId(), request);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @PostMapping("/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateBankAccount(
            Authentication auth,
            @RequestBody DeactivateAccountRequest bankId) {
        ApiResponse<Void> response = bankAccountService.deactivateBankAccount(auth.getName(), bankId.getBankId());
        return ResponseEntity.status(response.getStatusCode()).body(response);

    }

    @PostMapping("/activate")
    public ResponseEntity<ApiResponse<Void>> activateBankAccount(
            Authentication auth,
            @RequestBody DeactivateAccountRequest bankId) {
        ApiResponse<Void> response = bankAccountService.activateBankAccount(auth.getName(), bankId.getBankId());
        return ResponseEntity.status(response.getStatusCode()).body(response);

    }

    @DeleteMapping("/{bankId}")
    public ResponseEntity<ApiResponse<Void>> deleteBankAccount(
            Authentication auth,
            @PathVariable String bankId) {
        ApiResponse<Void> response = bankAccountService.deleteBankAccount(auth.getName(), bankId);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}