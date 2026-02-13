package com.financal.mgt.Financal.Management.repository.account;
import com.financal.mgt.Financal.Management.model.account.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    Optional<BankAccount> findByBankIdAndUserId(String bankId, String userId);

    List<BankAccount> findByUserIdOrderByCreatedAtDesc(String userId);

    Optional<BankAccount> findByUserId(String userId);

    boolean existsByBankAccountNumber(String accountNumber);

    boolean existsByBankAccountNumberAndUserId(String accountNumber, String userId);
}