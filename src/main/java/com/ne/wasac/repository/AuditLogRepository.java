package com.ne.wasac.repository;

import com.ne.wasac.enums.AuditAction;
import com.ne.wasac.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findAllByOrderByPerformedAtDesc();

    @Query("""
            SELECT a FROM AuditLog a
            WHERE (:actionType IS NULL OR a.actionType = :actionType)
              AND (:entityName IS NULL OR LOWER(a.entityName) = LOWER(:entityName))
              AND (:entityId IS NULL OR a.entityId = :entityId)
              AND (:userId IS NULL OR a.userId = :userId)
              AND (:from IS NULL OR a.performedAt >= :from)
              AND (:to IS NULL OR a.performedAt <= :to)
              AND (:q IS NULL OR LOWER(a.oldValue) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(a.newValue) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(a.entityName) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    List<AuditLog> search(
            @Param("actionType") AuditAction actionType,
            @Param("entityName") String entityName,
            @Param("entityId") Long entityId,
            @Param("userId") Long userId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("q") String q);
}
