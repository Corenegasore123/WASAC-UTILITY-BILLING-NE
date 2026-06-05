package com.ne.wasac.service;

import com.ne.wasac.dto.AuditLogResponse;
import com.ne.wasac.enums.AuditAction;
import com.ne.wasac.enums.SortDirection;
import com.ne.wasac.exception.ResourceNotFoundException;
import com.ne.wasac.model.AuditLog;
import com.ne.wasac.repository.AuditLogRepository;
import com.ne.wasac.util.QuerySort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private static final Map<String, Comparator<AuditLogResponse>> SORT_FIELDS = Map.of(
            "id", Comparator.comparing(AuditLogResponse::getId),
            "performedAt", Comparator.comparing(AuditLogResponse::getPerformedAt),
            "actionType", Comparator.comparing(a -> a.getActionType().name()),
            "entityName", Comparator.comparing(AuditLogResponse::getEntityName, String.CASE_INSENSITIVE_ORDER),
            "entityId", Comparator.comparing(AuditLogResponse::getEntityId, Comparator.nullsLast(Comparator.naturalOrder())),
            "userId", Comparator.comparing(AuditLogResponse::getUserId, Comparator.nullsLast(Comparator.naturalOrder())));

    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public List<AuditLogResponse> findAll(String sortBy, SortDirection sortDir) {
        return sort(auditLogRepository.findAllByOrderByPerformedAtDesc().stream()
                .map(this::toResponse).toList(), sortBy, sortDir);
    }

    @Transactional(readOnly = true)
    public AuditLogResponse findById(Long id) {
        return toResponse(auditLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Audit log not found: " + id)));
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> search(AuditAction actionType, String entityName, Long entityId,
                                         Long userId, LocalDateTime from, LocalDateTime to, String q,
                                         String sortBy, SortDirection sortDir) {
        return sort(auditLogRepository.search(actionType, QuerySort.blankToNull(entityName), entityId,
                        userId, from, to, QuerySort.blankToNull(q))
                .stream().map(this::toResponse).toList(), sortBy, sortDir);
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> filterAndSort(AuditAction actionType, String entityName, Long entityId,
                                                Long userId, LocalDateTime from, LocalDateTime to, String q,
                                                String sortBy, SortDirection sortDir) {
        if (actionType != null || QuerySort.blankToNull(entityName) != null || entityId != null
                || userId != null || from != null || to != null || QuerySort.blankToNull(q) != null) {
            return search(actionType, entityName, entityId, userId, from, to, q, sortBy, sortDir);
        }
        return findAll(sortBy, sortDir);
    }

    private List<AuditLogResponse> sort(List<AuditLogResponse> items, String sortBy, SortDirection sortDir) {
        return QuerySort.apply(items, sortBy, sortDir == null ? SortDirection.DESC : sortDir,
                SORT_FIELDS, "performedAt");
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return new AuditLogResponse(log.getId(), log.getUserId(), log.getActionType(),
                log.getEntityName(), log.getEntityId(), log.getOldValue(), log.getNewValue(),
                log.getPerformedAt());
    }
}
