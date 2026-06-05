package com.ne.wasac.model;

import com.ne.wasac.enums.AuditAction;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Immutable audit trail entry for sensitive business operations.
 * Stores who did what, on which entity, and when.
 */
@Getter
@Setter
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** User who performed the action (nullable for system jobs). */
    @Column(name = "user_id")
    private Long userId;

    /** High-level action category such as BILL_APPROVED. */
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false)
    private AuditAction actionType;

    /** Entity type name e.g. Bill, Customer. */
    @Column(name = "entity_name", nullable = false)
    private String entityName;

    /** Primary key of the affected entity. */
    @Column(name = "entity_id")
    private Long entityId;

    /** JSON or text snapshot before change. */
    @Column(name = "old_value", length = 1000)
    private String oldValue;

    /** JSON or text snapshot after change. */
    @Column(name = "new_value", length = 1000)
    private String newValue;

    /** Timestamp when the action was recorded. */
    @Column(name = "performed_at", nullable = false)
    private LocalDateTime performedAt = LocalDateTime.now();
}
