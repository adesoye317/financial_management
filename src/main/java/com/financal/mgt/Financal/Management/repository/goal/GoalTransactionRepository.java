package com.financal.mgt.Financal.Management.repository.goal;


import com.financal.mgt.Financal.Management.model.goal.GoalTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoalTransactionRepository extends JpaRepository<GoalTransaction, Long> {

    Page<GoalTransaction> findByGoalIdOrderByCreatedAtDesc(Long goalId, Pageable pageable);
}
