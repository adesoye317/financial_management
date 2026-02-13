package com.financal.mgt.Financal.Management.service.investment;

import com.financal.mgt.Financal.Management.dto.request.investment.*;
import com.financal.mgt.Financal.Management.dto.response.auth.ApiResponse;
import com.financal.mgt.Financal.Management.dto.response.investment.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InvestmentService {
    ApiResponse<Page<PoolResponse>> getPools(String riskLevel, String status, Pageable pageable);
    ApiResponse<PoolResponse> getPool(Long poolId);
    ApiResponse<InvestResultResponse> invest(String userId, InvestRequest request);
    ApiResponse<PortfolioResponse> getPortfolio(String userId);
    ApiResponse<UserInvestmentResponse> getInvestment(String userId, String investmentRef);
    ApiResponse<WithdrawResultResponse> withdrawInvestment(String userId, String investmentRef, WithdrawInvestmentRequest request);
    ApiResponse<Page<InvestmentTxnResponse>> getTransactions(String userId, Pageable pageable);
    // Admin
    ApiResponse<PoolResponse> createPool(CreatePoolRequest request);
    ApiResponse<PoolResponse> updatePoolStatus(Long poolId, String status);
}

