package com.financal.mgt.Financal.Management.repository.audit;

import com.financal.mgt.Financal.Management.model.audit.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
