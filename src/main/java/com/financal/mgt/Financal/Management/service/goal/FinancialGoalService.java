package com.financal.mgt.Financal.Management.service.goal;


import com.financal.mgt.Financal.Management.dto.response.auth.ApiResponse;
import com.financal.mgt.Financal.Management.model.goal.*;

public interface FinancialGoalService {
    ApiResponse<GoalResponse> createGoal(String userId, CreateGoalRequest request);
    ApiResponse<GoalResponse> getGoal(String userId, Long goalId);
    ApiResponse<GoalsSummaryResponse> getGoalsSummary(String userId);
    ApiResponse<GoalResponse> updateGoal(String userId, Long goalId, UpdateGoalRequest request);
    ApiResponse<GoalResponse> addFunds(String userId, Long goalId, AddFundsRequest request);
    ApiResponse<GoalResponse> withdrawFunds(String userId, Long goalId, AddFundsRequest request);
    ApiResponse<Void> cancelGoal(String userId, Long goalId);
}
