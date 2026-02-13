package com.financal.mgt.Financal.Management.model.goal;

import com.financal.mgt.Financal.Management.enums.goal.GoalCategory;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CreateGoalRequest {

    @NotBlank(message = "Goal name is required")
    private String goalName;

    @NotNull(message = "Category is required")
    private GoalCategory category;

    @NotNull(message = "Target amount is required")
    @DecimalMin(value = "100.00", message = "Minimum target is ₦100")
    private BigDecimal targetAmount;

    @NotNull(message = "Target date is required")
    @Future(message = "Target date must be in the future")
    private LocalDate targetDate;

    private String description;
}
