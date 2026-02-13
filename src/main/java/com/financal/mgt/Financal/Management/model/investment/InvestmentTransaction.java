package com.financal.mgt.Financal.Management.model.investment;


import com.financal.mgt.Financal.Management.enums.investment.InvestmentTxnType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "investment_transactions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InvestmentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String transactionRef;

    @Column(nullable = false)
    private Long userInvestmentId;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvestmentTxnType type;

    private String description;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (transactionRef == null) {
            transactionRef = "INVTXN-" + System.currentTimeMillis();
        }
    }
}

