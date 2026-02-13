package com.financal.mgt.Financal.Management.model.investment;


import com.financal.mgt.Financal.Management.enums.investment.InvestmentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_investments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserInvestment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String investmentRef;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private Long poolId;

    @Column(nullable = false)
    private String poolName;

    @Column(nullable = false)
    private String riskLevel;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal annualReturnPct;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amountInvested;

    @Column(nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal currentValue = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal returnsEarned = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private InvestmentStatus status = InvestmentStatus.ACTIVE;

    @Builder.Default
    private boolean autoReinvest = false;

    @Column(nullable = false)
    private LocalDate maturityDate;

    private LocalDateTime investedAt;
    private LocalDateTime withdrawnAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        investedAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (currentValue.compareTo(BigDecimal.ZERO) == 0) {
            currentValue = amountInvested;
        }
        if (investmentRef == null) {
            investmentRef = "INV-" + System.currentTimeMillis();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

