package com.financal.mgt.Financal.Management.repository.wallet;

import com.financal.mgt.Financal.Management.model.wallet.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByUserId(String userId);

    Optional<Wallet> findByAccountNumber(String accountNumber);

    // Pessimistic lock for safe balance updates
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.accountNumber = :accountNumber")
    Optional<Wallet> findByAccountNumberForUpdate(String accountNumber);

    boolean existsByUserId(String userId);
}
