package com.financal.mgt.Financal.Management.repository.wallet;

import com.financal.mgt.Financal.Management.model.wallet.TransactionLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TransactionLimitRepository extends JpaRepository<TransactionLimit, Long> {
    Optional<TransactionLimit> findByLimitKeyAndActiveTrue(String limitKey);
}
