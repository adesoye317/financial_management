package com.financal.mgt.Financal.Management.repository.investment;


import com.financal.mgt.Financal.Management.enums.investment.InvestmentStatus;
import com.financal.mgt.Financal.Management.model.investment.UserInvestment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface UserInvestmentRepository extends JpaRepository<UserInvestment, Long> {

    List<UserInvestment> findByUserIdOrderByInvestedAtDesc(String userId);

    List<UserInvestment> findByUserIdAndStatus(String userId, InvestmentStatus status);

    Optional<UserInvestment> findByInvestmentRefAndUserId(String investmentRef, String userId);

    @Query("SELECT COALESCE(SUM(i.amountInvested), 0) FROM UserInvestment i " +
            "WHERE i.userId = :userId AND i.status = 'ACTIVE'")
    BigDecimal getTotalActiveInvested(@Param("userId") String userId);

    @Query("SELECT COALESCE(SUM(i.currentValue), 0) FROM UserInvestment i " +
            "WHERE i.userId = :userId AND i.status = 'ACTIVE'")
    BigDecimal getTotalCurrentValue(@Param("userId") String userId);

    @Query("SELECT COALESCE(SUM(i.returnsEarned), 0) FROM UserInvestment i " +
            "WHERE i.userId = :userId")
    BigDecimal getTotalReturnsEarned(@Param("userId") String userId);

    // For ROI scheduler
    List<UserInvestment> findByStatus(InvestmentStatus status);
}