package com.financal.mgt.Financal.Management.service.wallet;


import com.financal.mgt.Financal.Management.dto.request.wallet.FundWalletRequest;
import com.financal.mgt.Financal.Management.dto.request.wallet.TransferRequest;
import com.financal.mgt.Financal.Management.dto.request.wallet.WithdrawRequest;
import com.financal.mgt.Financal.Management.dto.response.auth.ApiResponse;
import com.financal.mgt.Financal.Management.dto.response.wallet.TransactionResponse;
import com.financal.mgt.Financal.Management.dto.response.wallet.WalletResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface WalletService {
    ApiResponse<WalletResponse> createWallet(String userId, String accountHolderName);
    ApiResponse<WalletResponse> getWallet(String userId);
    ApiResponse<WalletResponse> fundWallet(String userId, FundWalletRequest request);
    ApiResponse<WalletResponse> transfer(String userId, TransferRequest request);
    ApiResponse<WalletResponse> withdraw(String userId, WithdrawRequest request);
    ApiResponse<Page<TransactionResponse>> getTransactions(String userId, Pageable pageable);
}