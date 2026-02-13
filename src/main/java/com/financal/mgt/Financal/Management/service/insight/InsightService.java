package com.financal.mgt.Financal.Management.service.insight;

import com.financal.mgt.Financal.Management.dto.response.auth.ApiResponse;
import com.financal.mgt.Financal.Management.dto.response.insight.*;

public interface InsightService {
    ApiResponse<DashboardResponse> getDashboard(String userId);
    ApiResponse<SpendingAnalysisResponse> getSpendingAnalysis(String userId, String period, int months);
    ApiResponse<IncomeAnalysisResponse> getIncomeAnalysis(String userId, String period, int months);
    ApiResponse<InvestmentInsightResponse> getInvestmentInsights(String userId);
    ApiResponse<GoalInsightResponse> getGoalInsights(String userId);
    ApiResponse<RecommendationResponse> getRecommendations(String userId);
    ApiResponse<CashFlowResponse> getCashFlowForecast(String userId, int months);
}
