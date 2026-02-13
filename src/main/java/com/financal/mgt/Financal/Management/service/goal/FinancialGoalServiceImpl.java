package com.financal.mgt.Financal.Management.service.goal;

import com.financal.mgt.Financal.Management.dto.response.auth.ApiResponse;
import com.financal.mgt.Financal.Management.enums.goal.GoalStatus;
import com.financal.mgt.Financal.Management.model.goal.*;
import com.financal.mgt.Financal.Management.repository.goal.FinancialGoalRepository;
import com.financal.mgt.Financal.Management.repository.goal.GoalTransactionRepository;
import com.financal.mgt.Financal.Management.service.audit.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinancialGoalServiceImpl implements FinancialGoalService {

    private final FinancialGoalRepository goalRepository;
    private final GoalTransactionRepository goalTransactionRepository;
    private final AuditService auditService;

    @Override
    @Transactional
    public ApiResponse<GoalResponse> createGoal(String userId, CreateGoalRequest request) {
        try {
            FinancialGoal goal = FinancialGoal.builder()
                    .userId(userId)
                    .goalName(request.getGoalName())
                    .category(request.getCategory())
                    .targetAmount(request.getTargetAmount())
                    .savedAmount(BigDecimal.ZERO)
                    .status(GoalStatus.ACTIVE)
                    .targetDate(request.getTargetDate())
                    .description(request.getDescription())
                    .build();

            goal = goalRepository.save(goal);

            auditService.log(userId, "CREATE_GOAL",
                    "Created goal '" + goal.getGoalName() + "' with target ₦" + goal.getTargetAmount(),
                    null);

            log.info("Goal created: userId={}, goalName={}, target={}",
                    userId, goal.getGoalName(), goal.getTargetAmount());

            return new ApiResponse<>(200, "Goal created successfully", mapToResponse(goal));

        } catch (Exception e) {
            log.error("Failed to create goal: {}", e.getMessage(), e);
            return new ApiResponse<>(500, "Unable to create goal. Please try again later.", null);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<GoalResponse> getGoal(String userId, Long goalId) {
        try {
            FinancialGoal goal = goalRepository.findByIdAndUserId(goalId, userId)
                    .orElse(null);

            if (goal == null) {
                return new ApiResponse<>(404, "Goal not found", null);
            }

            return new ApiResponse<>(200, "Goal retrieved successfully", mapToResponse(goal));

        } catch (Exception e) {
            log.error("Failed to get goal: {}", e.getMessage(), e);
            return new ApiResponse<>(500, "Unable to retrieve goal. Please try again later.", null);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<GoalsSummaryResponse> getGoalsSummary(String userId) {
        try {
            List<FinancialGoal> goals = goalRepository.findByUserIdOrderByCreatedAtDesc(userId);

            int activeGoals = goalRepository.countByUserIdAndStatus(userId, GoalStatus.ACTIVE);
            int achievedGoals = goalRepository.countByUserIdAndStatus(userId, GoalStatus.ACHIEVED);
            BigDecimal totalSaved = goalRepository.getTotalSaved(userId);
            BigDecimal totalTarget = goalRepository.getTotalTargetForActive(userId);

            BigDecimal overallProgress = BigDecimal.ZERO;
            if (totalTarget.compareTo(BigDecimal.ZERO) > 0) {
                overallProgress = totalSaved
                        .multiply(new BigDecimal("100"))
                        .divide(totalTarget, 1, RoundingMode.HALF_UP);
            }

            List<GoalResponse> goalResponses = goals.stream()
                    .map(this::mapToResponse)
                    .toList();

            GoalsSummaryResponse summary = GoalsSummaryResponse.builder()
                    .activeGoals(activeGoals)
                    .totalSaved(totalSaved)
                    .goalsAchieved(achievedGoals)
                    .totalTarget(totalTarget)
                    .overallProgressPercentage(overallProgress)
                    .goals(goalResponses)
                    .build();

            return new ApiResponse<>(200, "Goals summary retrieved successfully", summary);

        } catch (Exception e) {
            log.error("Failed to get goals summary: {}", e.getMessage(), e);
            return new ApiResponse<>(500, "Unable to retrieve goals summary. Please try again later.", null);
        }
    }

    @Override
    @Transactional
    public ApiResponse<GoalResponse> updateGoal(String userId, Long goalId, UpdateGoalRequest request) {
        try {
            FinancialGoal goal = goalRepository.findByIdAndUserId(goalId, userId)
                    .orElse(null);

            if (goal == null) {
                return new ApiResponse<>(404, "Goal not found", null);
            }

            if (goal.getStatus() != GoalStatus.ACTIVE) {
                return new ApiResponse<>(400, "Can only edit active goals", null);
            }

            if (request.getGoalName() != null) goal.setGoalName(request.getGoalName());
            if (request.getCategory() != null) goal.setCategory(request.getCategory());
            if (request.getTargetDate() != null) goal.setTargetDate(request.getTargetDate());
            if (request.getDescription() != null) goal.setDescription(request.getDescription());

            if (request.getTargetAmount() != null) {
                if (request.getTargetAmount().compareTo(goal.getSavedAmount()) < 0) {
                    return new ApiResponse<>(400,
                            "Target amount cannot be less than already saved amount of ₦"
                                    + goal.getSavedAmount().toPlainString(), null);
                }
                goal.setTargetAmount(request.getTargetAmount());
            }

            goal = goalRepository.save(goal);

            auditService.log(userId, "UPDATE_GOAL",
                    "Updated goal '" + goal.getGoalName() + "'", null);

            return new ApiResponse<>(200, "Goal updated successfully", mapToResponse(goal));

        } catch (Exception e) {
            log.error("Failed to update goal: {}", e.getMessage(), e);
            return new ApiResponse<>(500, "Unable to update goal. Please try again later.", null);
        }
    }

    @Override
    @Transactional
    public ApiResponse<GoalResponse> addFunds(String userId, Long goalId, AddFundsRequest request) {
        try {
            FinancialGoal goal = goalRepository.findByIdAndUserIdForUpdate(goalId, userId)
                    .orElse(null);

            if (goal == null) {
                return new ApiResponse<>(404, "Goal not found", null);
            }

            if (goal.getStatus() != GoalStatus.ACTIVE) {
                return new ApiResponse<>(400, "Can only add funds to active goals", null);
            }

            BigDecimal remaining = goal.getRemainingAmount();
            BigDecimal amountToAdd = request.getAmount();

            if (remaining.compareTo(BigDecimal.ZERO) == 0) {
                return new ApiResponse<>(400, "Goal is already fully funded", null);
            }

            // Cap at target — don't overfund
            if (amountToAdd.compareTo(remaining) > 0) {
                amountToAdd = remaining;
            }

            goal.setSavedAmount(goal.getSavedAmount().add(amountToAdd));

            String message = "Funds added successfully";

            // Auto-achieve if target is met
            if (goal.isCompleted()) {
                goal.setStatus(GoalStatus.ACHIEVED);
                message = "Funds added successfully. Congratulations! Goal achieved!";
                log.info("Goal achieved: userId={}, goalName={}", userId, goal.getGoalName());
            }

            goalRepository.save(goal);

            GoalTransaction txn = GoalTransaction.builder()
                    .goalId(goalId)
                    .userId(userId)
                    .amount(amountToAdd)
                    .type("DEPOSIT")
                    .description(request.getDescription() != null
                            ? request.getDescription()
                            : "Funds added to " + goal.getGoalName())
                    .build();
            goalTransactionRepository.save(txn);

            auditService.log(userId, "GOAL_ADD_FUNDS",
                    "Added ₦" + amountToAdd + " to goal '" + goal.getGoalName()
                            + "'. Progress: " + goal.getProgressPercentage() + "%",
                    null);

            log.info("Funds added to goal: userId={}, goalId={}, amount={}, progress={}%",
                    userId, goalId, amountToAdd, goal.getProgressPercentage());

            return new ApiResponse<>(200, message, mapToResponse(goal));

        } catch (Exception e) {
            log.error("Failed to add funds to goal: {}", e.getMessage(), e);
            return new ApiResponse<>(500, "Unable to add funds. Please try again later.", null);
        }
    }

    @Override
    @Transactional
    public ApiResponse<GoalResponse> withdrawFunds(String userId, Long goalId, AddFundsRequest request) {
        try {
            FinancialGoal goal = goalRepository.findByIdAndUserIdForUpdate(goalId, userId)
                    .orElse(null);

            if (goal == null) {
                return new ApiResponse<>(404, "Goal not found", null);
            }

            if (goal.getStatus() == GoalStatus.CANCELLED) {
                return new ApiResponse<>(400, "Cannot withdraw from a cancelled goal", null);
            }

            if (request.getAmount().compareTo(goal.getSavedAmount()) > 0) {
                return new ApiResponse<>(400,
                        "Withdraw amount exceeds saved amount of ₦"
                                + goal.getSavedAmount().toPlainString(), null);
            }

            goal.setSavedAmount(goal.getSavedAmount().subtract(request.getAmount()));

            // If it was achieved and now funds withdrawn, revert to active
            if (goal.getStatus() == GoalStatus.ACHIEVED && !goal.isCompleted()) {
                goal.setStatus(GoalStatus.ACTIVE);
            }

            goalRepository.save(goal);

            GoalTransaction txn = GoalTransaction.builder()
                    .goalId(goalId)
                    .userId(userId)
                    .amount(request.getAmount())
                    .type("WITHDRAWAL")
                    .description(request.getDescription() != null
                            ? request.getDescription()
                            : "Funds withdrawn from " + goal.getGoalName())
                    .build();
            goalTransactionRepository.save(txn);

            auditService.log(userId, "GOAL_WITHDRAW_FUNDS",
                    "Withdrew ₦" + request.getAmount() + " from goal '" + goal.getGoalName() + "'",
                    null);

            return new ApiResponse<>(200, "Funds withdrawn successfully", mapToResponse(goal));

        } catch (Exception e) {
            log.error("Failed to withdraw funds from goal: {}", e.getMessage(), e);
            return new ApiResponse<>(500, "Unable to withdraw funds. Please try again later.", null);
        }
    }

    @Override
    @Transactional
    public ApiResponse<Void> cancelGoal(String userId, Long goalId) {
        try {
            FinancialGoal goal = goalRepository.findByIdAndUserId(goalId, userId)
                    .orElse(null);

            if (goal == null) {
                return new ApiResponse<>(404, "Goal not found", null);
            }

            if (goal.getSavedAmount().compareTo(BigDecimal.ZERO) > 0) {
                return new ApiResponse<>(400,
                        "Withdraw saved funds (₦" + goal.getSavedAmount().toPlainString()
                                + ") before cancelling the goal", null);
            }

            goal.setStatus(GoalStatus.CANCELLED);
            goalRepository.save(goal);

            auditService.log(userId, "CANCEL_GOAL",
                    "Cancelled goal '" + goal.getGoalName() + "'", null);

            log.info("Goal cancelled: userId={}, goalId={}", userId, goalId);

            return new ApiResponse<>(200, "Goal cancelled successfully", null);

        } catch (Exception e) {
            log.error("Failed to cancel goal: {}", e.getMessage(), e);
            return new ApiResponse<>(500, "Unable to cancel goal. Please try again later.", null);
        }
    }

    // ==================== HELPER ====================

    private GoalResponse mapToResponse(FinancialGoal goal) {
        return GoalResponse.builder()
                .id(goal.getId())
                .goalName(goal.getGoalName())
                .category(goal.getCategory().name())
                .targetAmount(goal.getTargetAmount())
                .savedAmount(goal.getSavedAmount())
                .remainingAmount(goal.getRemainingAmount())
                .progressPercentage(goal.getProgressPercentage())
                .status(goal.getStatus().name())
                .targetDate(goal.getTargetDate())
                .description(goal.getDescription())
                .createdAt(goal.getCreatedAt())
                .build();
    }
}