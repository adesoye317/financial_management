package com.financal.mgt.Financal.Management.controller.wallet;

import com.financal.mgt.Financal.Management.dto.request.wallet.FundWalletRequest;
import com.financal.mgt.Financal.Management.dto.request.wallet.TransferRequest;
import com.financal.mgt.Financal.Management.dto.request.wallet.WithdrawRequest;
import com.financal.mgt.Financal.Management.dto.response.auth.ApiResponse;
import com.financal.mgt.Financal.Management.dto.response.wallet.TransactionResponse;
import com.financal.mgt.Financal.Management.dto.response.wallet.WalletResponse;
import com.financal.mgt.Financal.Management.service.wallet.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<WalletResponse>> createWallet(
            Authentication auth,
            @RequestBody Map<String, String> body) {
        String userId = auth.getName();
        String holderName = body.getOrDefault("accountHolderName", userId);
        ApiResponse<WalletResponse> response = walletService.createWallet(userId, holderName);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<WalletResponse>> getWallet(Authentication auth) {
        ApiResponse<WalletResponse> response = walletService.getWallet(auth.getName());
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @PostMapping("/fund")
    public ResponseEntity<ApiResponse<WalletResponse>> fundWallet(
            Authentication auth,
            @Valid @RequestBody FundWalletRequest request) {
        ApiResponse<WalletResponse> response = walletService.fundWallet(auth.getName(), request);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<WalletResponse>> transfer(
            Authentication auth,
            @Valid @RequestBody TransferRequest request) {
        ApiResponse<WalletResponse> response = walletService.transfer(auth.getName(), request);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<WalletResponse>> withdraw(
            Authentication auth,
            @Valid @RequestBody WithdrawRequest request) {
        ApiResponse<WalletResponse> response = walletService.withdraw(auth.getName(), request);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getTransactions(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        ApiResponse<Page<TransactionResponse>> response = walletService.getTransactions(
                auth.getName(), PageRequest.of(page, size));
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}