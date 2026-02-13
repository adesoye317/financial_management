package com.financal.mgt.Financal.Management.repository.investment;



import com.financal.mgt.Financal.Management.enums.investment.PoolStatus;
import com.financal.mgt.Financal.Management.enums.investment.RiskLevel;
import com.financal.mgt.Financal.Management.model.investment.InvestmentPool;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InvestmentPoolRepository extends JpaRepository<InvestmentPool, Long> {

    Page<InvestmentPool> findByStatus(PoolStatus status, Pageable pageable);

    Page<InvestmentPool> findByStatusAndRiskLevel(PoolStatus status, RiskLevel riskLevel, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM InvestmentPool p WHERE p.id = :id")
    Optional<InvestmentPool> findByIdForUpdate(@Param("id") Long id);
}
