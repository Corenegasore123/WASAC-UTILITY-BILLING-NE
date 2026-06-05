package com.ne.wasac.dto;

import com.ne.wasac.enums.AuditAction;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AuditLogResponse {

    private Long id;
    private Long userId;
    private AuditAction actionType;
    private String entityName;
    private Long entityId;
    private String oldValue;
    private String newValue;
    private LocalDateTime performedAt;
}
