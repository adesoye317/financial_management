package com.financal.mgt.Financal.Management.model.investment;


import com.financal.mgt.Financal.Management.enums.investment.PoolStatus;
import com.financal.mgt.Financal.Management.enums.investment.RiskLevel;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "investment_pools")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InvestmentPool {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String poolName;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskLevel riskLevel;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal annualReturnPct;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal minInvestment;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal maxInvestment;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalPoolSize;

    @Column(nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal currentInvested = BigDecimal.ZERO;

    @Column(nullable = false)
    private int durationMonths;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PoolStatus status = PoolStatus.OPEN;

    private LocalDate maturityDate;

    @Column(length = 2000)
    private String terms;

    @Column(nullable = false, precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal earlyWithdrawalPenaltyPct = new BigDecimal("2.00");

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public BigDecimal getAvailableSlots() {
        return totalPoolSize.subtract(currentInvested);
    }
}

