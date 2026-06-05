package com.ne.wasac.controller;

import com.ne.wasac.dto.AuditLogResponse;
import com.ne.wasac.enums.AuditAction;
import com.ne.wasac.enums.SortDirection;
import com.ne.wasac.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Audit Logs")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @Operation(summary = "List audit logs",
            description = "sortBy: id, performedAt, actionType, entityName, entityId, userId. Default: performedAt DESC.")
    public List<AuditLogResponse> findAll(
            @RequestParam(required = false) AuditAction actionType,
            @RequestParam(required = false) String entityName,
            @RequestParam(required = false) Long entityId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false, defaultValue = "DESC") SortDirection sortDir) {
        return auditLogService.filterAndSort(actionType, entityName, entityId, userId, from, to, q, sortBy, sortDir);
    }

    @GetMapping("/search")
    @Operation(summary = "Search audit logs", description = "Filter + sort audit trail. Role: ADMIN.")
    public List<AuditLogResponse> search(
            @Parameter(description = "Action type") @RequestParam(required = false) AuditAction actionType,
            @Parameter(description = "Entity name") @RequestParam(required = false) String entityName,
            @Parameter(description = "Entity id") @RequestParam(required = false) Long entityId,
            @Parameter(description = "Performer user id") @RequestParam(required = false) Long userId,
            @Parameter(description = "From datetime") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "To datetime") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @Parameter(description = "Keyword") @RequestParam(required = false) String q,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false, defaultValue = "DESC") SortDirection sortDir) {
        return auditLogService.search(actionType, entityName, entityId, userId, from, to, q, sortBy, sortDir);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get audit log by id", description = "Role: ADMIN.")
    public AuditLogResponse findById(@PathVariable Long id) {
        return auditLogService.findById(id);
    }
}
