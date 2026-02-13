package com.financal.mgt.Financal.Management.model.goal;


import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GoalResponse {
    private Long id;
    private String goalName;
    private String category;
    private BigDecimal targetAmount;
    private BigDecimal savedAmount;
    private BigDecimal remainingAmount;
    private BigDecimal progressPercentage;
    private String status;
    private LocalDate targetDate;
    private String description;
    private LocalDateTime createdAt;
}
