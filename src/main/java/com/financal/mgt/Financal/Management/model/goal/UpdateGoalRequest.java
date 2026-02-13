package com.financal.mgt.Financal.Management.model.goal;

import com.financal.mgt.Financal.Management.enums.goal.GoalCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class UpdateGoalRequest {
    private String goalName;
    private GoalCategory category;

    @DecimalMin(value = "100.00", message = "Minimum target is ₦100")
    private BigDecimal targetAmount;

    @Future(message = "Target date must be in the future")
    private LocalDate targetDate;

    private String description;
}
