package com.financal.mgt.Financal.Management.model.goal;


import com.financal.mgt.Financal.Management.enums.goal.GoalCategory;
import com.financal.mgt.Financal.Management.enums.goal.GoalStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "financial_goals")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FinancialGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String goalName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GoalCategory category;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal targetAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal savedAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GoalStatus status;

    @Column(nullable = false)
    private LocalDate targetDate;

    private String description;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (savedAmount == null) savedAmount = BigDecimal.ZERO;
        if (status == null) status = GoalStatus.ACTIVE;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public BigDecimal getRemainingAmount() {
        return targetAmount.subtract(savedAmount).max(BigDecimal.ZERO);
    }

    public BigDecimal getProgressPercentage() {
        if (targetAmount.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return savedAmount
                .multiply(new BigDecimal("100"))
                .divide(targetAmount, 1, RoundingMode.HALF_UP);
    }

    public boolean isCompleted() {
        return savedAmount.compareTo(targetAmount) >= 0;
    }
}
