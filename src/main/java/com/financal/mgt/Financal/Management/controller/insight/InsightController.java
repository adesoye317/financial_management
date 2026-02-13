package com.financal.mgt.Financal.Management.controller.insight;


import com.financal.mgt.Financal.Management.dto.response.auth.ApiResponse;
import com.financal.mgt.Financal.Management.dto.response.insight.*;
import com.financal.mgt.Financal.Management.service.insight.InsightService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/insights")
@RequiredArgsConstructor
public class InsightController {

    private final InsightService insightService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard(Authentication auth) {
        ApiResponse<DashboardResponse> response = insightService.getDashboard(auth.getName());
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @GetMapping("/spending")
    public ResponseEntity<ApiResponse<SpendingAnalysisResponse>> getSpending(
            Authentication auth,
            @RequestParam(defaultValue = "monthly") String period,
            @RequestParam(defaultValue = "6") int months) {
        ApiResponse<SpendingAnalysisResponse> response = insightService.getSpendingAnalysis(auth.getName(), period, months);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @GetMapping("/income")
    public ResponseEntity<ApiResponse<IncomeAnalysisResponse>> getIncome(
            Authentication auth,
            @RequestParam(defaultValue = "monthly") String period,
            @RequestParam(defaultValue = "6") int months) {
        ApiResponse<IncomeAnalysisResponse> response = insightService.getIncomeAnalysis(auth.getName(), period, months);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @GetMapping("/investments")
    public ResponseEntity<ApiResponse<InvestmentInsightResponse>> getInvestments(Authentication auth) {
        ApiResponse<InvestmentInsightResponse> response = insightService.getInvestmentInsights(auth.getName());
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @GetMapping("/goals")
    public ResponseEntity<ApiResponse<GoalInsightResponse>> getGoals(Authentication auth) {
        ApiResponse<GoalInsightResponse> response = insightService.getGoalInsights(auth.getName());
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @GetMapping("/recommendations")
    public ResponseEntity<ApiResponse<RecommendationResponse>> getRecommendations(Authentication auth) {
        ApiResponse<RecommendationResponse> response = insightService.getRecommendations(auth.getName());
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @GetMapping("/cashflow")
    public ResponseEntity<ApiResponse<CashFlowResponse>> getCashFlow(
            Authentication auth,
            @RequestParam(defaultValue = "3") int months) {
        ApiResponse<CashFlowResponse> response = insightService.getCashFlowForecast(auth.getName(), months);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}
