package com.financal.mgt.Financal.Management.controller.admin;


import com.financal.mgt.Financal.Management.dto.request.investment.CreatePoolRequest;
import com.financal.mgt.Financal.Management.dto.response.auth.ApiResponse;
import com.financal.mgt.Financal.Management.dto.response.investment.PoolResponse;
import com.financal.mgt.Financal.Management.service.investment.InvestmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/investments")
@RequiredArgsConstructor
public class AdminInvestmentController {

    private final InvestmentService investmentService;

    @PostMapping("/pools")
    public ResponseEntity<ApiResponse<PoolResponse>> createPool(
            @Valid @RequestBody CreatePoolRequest request) {
        ApiResponse<PoolResponse> response = investmentService.createPool(request);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @PatchMapping("/pools/{poolId}/status")
    public ResponseEntity<ApiResponse<PoolResponse>> updatePoolStatus(
            @PathVariable Long poolId,
            @RequestBody Map<String, String> body) {
        ApiResponse<PoolResponse> response = investmentService.updatePoolStatus(poolId, body.get("status"));
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}
