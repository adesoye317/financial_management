package com.financal.mgt.Financal.Management.model.account;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "bank_account")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String bankId;

    @Column(unique = true, nullable = false)
    private String bankAccountNumber;

    @Column(nullable = false)
    private String bankName;

    @Column(nullable = false)
    private String accountHolderName;

    @Builder.Default
    private String accountStatus = "ACTIVE"; // ACTIVE, INACTIVE, SUSPENDED

    @Column(nullable = false)
    private String userId;

    private String bankCode;       // e.g. "058" for GTBank, "033" for UBA
    private String accountType;    // SAVINGS, CURRENT

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (accountStatus == null) accountStatus = "ACTIVE";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}