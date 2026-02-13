package com.financal.mgt.Financal.Management.service.audit;



import com.financal.mgt.Financal.Management.model.audit.AuditLog;
import com.financal.mgt.Financal.Management.repository.audit.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Async
    public void log(String userId, String action, String details, String ipAddress) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .userId(userId)
                    .action(action)
                    .details(details)
                    .ipAddress(ipAddress)
                    .build();
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            // Audit failure should never break a transaction
            log.error("Failed to save audit log: {}", e.getMessage());
        }
    }
}
