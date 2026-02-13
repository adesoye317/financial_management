package com.financal.mgt.Financal.Management.repository.investment;


import com.financal.mgt.Financal.Management.model.investment.InvestmentTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvestmentTransactionRepository extends JpaRepository<InvestmentTransaction, Long> {

    Page<InvestmentTransaction> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    List<InvestmentTransaction> findByUserInvestmentIdOrderByCreatedAtDesc(Long userInvestmentId);
}

