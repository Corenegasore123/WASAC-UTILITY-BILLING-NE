package com.ne.wasac.service;

import com.ne.wasac.enums.AuditAction;
import com.ne.wasac.model.AuditLog;
import com.ne.wasac.repository.AuditLogRepository;
import com.ne.wasac.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Records who changed what for compliance and troubleshooting.
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Persists an audit row with current user id when available.
     */
    @Transactional
    public void log(AuditAction action, String entityName, Long entityId, String oldValue, String newValue) {
        AuditLog entry = new AuditLog();
        try {
            entry.setUserId(SecurityUtils.currentUser().getUser().getId());
        } catch (Exception ignored) {
            entry.setUserId(null);
        }
        entry.setActionType(action);
        entry.setEntityName(entityName);
        entry.setEntityId(entityId);
        entry.setOldValue(oldValue);
        entry.setNewValue(newValue);
        entry.setPerformedAt(LocalDateTime.now());
        auditLogRepository.save(entry);
    }
}
