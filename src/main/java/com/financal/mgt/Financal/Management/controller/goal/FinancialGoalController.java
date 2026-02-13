package com.financal.mgt.Financal.Management.controller.goal;


import com.financal.mgt.Financal.Management.dto.response.auth.ApiResponse;
import com.financal.mgt.Financal.Management.model.goal.*;
import com.financal.mgt.Financal.Management.service.goal.FinancialGoalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/goals")
@RequiredArgsConstructor
public class FinancialGoalController {

    private final FinancialGoalService goalService;

    @PostMapping
    public ResponseEntity<ApiResponse<GoalResponse>> createGoal(
            Authentication auth,
            @Valid @RequestBody CreateGoalRequest request) {
        ApiResponse<GoalResponse> response = goalService.createGoal(auth.getName(), request);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @GetMapping("/{goalId}")
    public ResponseEntity<ApiResponse<GoalResponse>> getGoal(
            Authentication auth,
            @PathVariable Long goalId) {
        ApiResponse<GoalResponse> response = goalService.getGoal(auth.getName(), goalId);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<GoalsSummaryResponse>> getSummary(Authentication auth) {
        ApiResponse<GoalsSummaryResponse> response = goalService.getGoalsSummary(auth.getName());
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @PutMapping("/{goalId}")
    public ResponseEntity<ApiResponse<GoalResponse>> updateGoal(
            Authentication auth,
            @PathVariable Long goalId,
            @Valid @RequestBody UpdateGoalRequest request) {
        ApiResponse<GoalResponse> response = goalService.updateGoal(auth.getName(), goalId, request);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @PostMapping("/{goalId}/fund")
    public ResponseEntity<ApiResponse<GoalResponse>> addFunds(
            Authentication auth,
            @PathVariable Long goalId,
            @Valid @RequestBody AddFundsRequest request) {
        ApiResponse<GoalResponse> response = goalService.addFunds(auth.getName(), goalId, request);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @PostMapping("/{goalId}/withdraw")
    public ResponseEntity<ApiResponse<GoalResponse>> withdrawFunds(
            Authentication auth,
            @PathVariable Long goalId,
            @Valid @RequestBody AddFundsRequest request) {
        ApiResponse<GoalResponse> response = goalService.withdrawFunds(auth.getName(), goalId, request);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @DeleteMapping("/{goalId}")
    public ResponseEntity<ApiResponse<Void>> cancelGoal(
            Authentication auth,
            @PathVariable Long goalId) {
        ApiResponse<Void> response = goalService.cancelGoal(auth.getName(), goalId);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}