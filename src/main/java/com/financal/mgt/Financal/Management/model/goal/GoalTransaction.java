package com.financal.mgt.Financal.Management.model.goal;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "goal_transactions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GoalTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String transactionRef;

    @Column(nullable = false)
    private Long goalId;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String type; // DEPOSIT, WITHDRAWAL

    private String description;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (transactionRef == null) {
            transactionRef = "GOAL-TXN-" + System.currentTimeMillis();
        }
    }
}

