package com.financal.mgt.Financal.Management.service.account;



import com.financal.mgt.Financal.Management.dto.request.account.AddBankAccountRequest;
import com.financal.mgt.Financal.Management.dto.request.account.UpdateBankAccountRequest;
import com.financal.mgt.Financal.Management.dto.response.account.BankAccountResponse;
import com.financal.mgt.Financal.Management.dto.response.auth.ApiResponse;

import java.util.List;

public interface BankAccountService {
    ApiResponse<BankAccountResponse> addBankAccount(String userId, AddBankAccountRequest request);
    ApiResponse<List<BankAccountResponse>> getUserBankAccounts(String userId);
    ApiResponse<BankAccountResponse> getBankAccount(String userId, String bankId);
    ApiResponse<BankAccountResponse> updateBankAccount(String userId, String bankId, UpdateBankAccountRequest request);
    ApiResponse<Void> deactivateBankAccount(String userId, String bankId);
    ApiResponse<Void> activateBankAccount(String userId, String bankId);
    ApiResponse<Void> deleteBankAccount(String userId, String bankId);
}