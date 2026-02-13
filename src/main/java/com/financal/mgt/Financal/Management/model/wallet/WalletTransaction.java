package com.financal.mgt.Financal.Management.model.wallet;


import com.financal.mgt.Financal.Management.enums.wallet.TransactionStatus;
import com.financal.mgt.Financal.Management.enums.wallet.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallet_transactions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String transactionRef;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionType type;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    private String description;
    private String senderAccountNumber;
    private String receiverAccountNumber;

    @Column(nullable = false)
    private String walletAccountNumber;

    private LocalDateTime createdAt;

    @Column(unique = true)
    private String idempotencyKey;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (transactionRef == null) {
            transactionRef = "TXN-" + System.currentTimeMillis();
        }
    }
}
