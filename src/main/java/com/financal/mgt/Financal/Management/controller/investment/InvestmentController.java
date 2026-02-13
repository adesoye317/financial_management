package com.financal.mgt.Financal.Management.controller.investment;


import com.financal.mgt.Financal.Management.dto.request.investment.*;
import com.financal.mgt.Financal.Management.dto.response.auth.ApiResponse;
import com.financal.mgt.Financal.Management.dto.response.investment.*;
import com.financal.mgt.Financal.Management.service.investment.InvestmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/investments")
@RequiredArgsConstructor
public class InvestmentController {

    private final InvestmentService investmentService;

    @GetMapping("/pools")
    public ResponseEntity<ApiResponse<Page<PoolResponse>>> getPools(
            @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false, defaultValue = "OPEN") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        ApiResponse<Page<PoolResponse>> response = investmentService.getPools(
                riskLevel, status, PageRequest.of(page, size));
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @GetMapping("/pools/{poolId}")
    public ResponseEntity<ApiResponse<PoolResponse>> getPool(@PathVariable Long poolId) {
        ApiResponse<PoolResponse> response = investmentService.getPool(poolId);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @PostMapping("/invest")
    public ResponseEntity<ApiResponse<InvestResultResponse>> invest(
            Authentication auth,
            @Valid @RequestBody InvestRequest request) {
        ApiResponse<InvestResultResponse> response = investmentService.invest(auth.getName(), request);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @GetMapping("/portfolio")
    public ResponseEntity<ApiResponse<PortfolioResponse>> getPortfolio(Authentication auth) {
        ApiResponse<PortfolioResponse> response = investmentService.getPortfolio(auth.getName());
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @GetMapping("/{investmentRef}")
    public ResponseEntity<ApiResponse<UserInvestmentResponse>> getInvestment(
            Authentication auth,
            @PathVariable String investmentRef) {
        ApiResponse<UserInvestmentResponse> response = investmentService.getInvestment(auth.getName(), investmentRef);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @PostMapping("/{investmentRef}/withdraw")
    public ResponseEntity<ApiResponse<WithdrawResultResponse>> withdraw(
            Authentication auth,
            @PathVariable String investmentRef,
            @RequestBody(required = false) WithdrawInvestmentRequest request) {
        if (request == null) request = new WithdrawInvestmentRequest();
        ApiResponse<WithdrawResultResponse> response = investmentService.withdrawInvestment(
                auth.getName(), investmentRef, request);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<Page<InvestmentTxnResponse>>> getTransactions(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        ApiResponse<Page<InvestmentTxnResponse>> response = investmentService.getTransactions(
                auth.getName(), PageRequest.of(page, size));
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}

