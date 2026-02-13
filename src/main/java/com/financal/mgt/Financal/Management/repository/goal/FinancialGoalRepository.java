package com.financal.mgt.Financal.Management.repository.goal;


import com.financal.mgt.Financal.Management.enums.goal.GoalStatus;
import com.financal.mgt.Financal.Management.model.goal.FinancialGoal;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface FinancialGoalRepository extends JpaRepository<FinancialGoal, Long> {

    List<FinancialGoal> findByUserIdOrderByCreatedAtDesc(String userId);

    List<FinancialGoal> findByUserIdAndStatus(String userId, GoalStatus status);

    Optional<FinancialGoal> findByIdAndUserId(Long id, String userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM FinancialGoal g WHERE g.id = :id AND g.userId = :userId")
    Optional<FinancialGoal> findByIdAndUserIdForUpdate(
            @Param("id") Long id, @Param("userId") String userId);

    @Query("SELECT COUNT(g) FROM FinancialGoal g WHERE g.userId = :userId AND g.status = :status")
    int countByUserIdAndStatus(@Param("userId") String userId, @Param("status") GoalStatus status);

    @Query("SELECT COALESCE(SUM(g.savedAmount), 0) FROM FinancialGoal g WHERE g.userId = :userId")
    BigDecimal getTotalSaved(@Param("userId") String userId);

    @Query("SELECT COALESCE(SUM(g.targetAmount), 0) FROM FinancialGoal g " +
            "WHERE g.userId = :userId AND g.status = 'ACTIVE'")
    BigDecimal getTotalTargetForActive(@Param("userId") String userId);
}
