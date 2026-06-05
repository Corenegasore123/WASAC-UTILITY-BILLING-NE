package com.ne.wasac.controller;

import com.ne.wasac.dto.NotificationResponse;
import com.ne.wasac.enums.SortDirection;
import com.ne.wasac.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE','OPERATOR')")
    @Operation(summary = "List or filter notifications",
            description = "Staff only. sortBy: id, createdAt, eventType, customerId, emailSent.")
    public List<NotificationResponse> findAll(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) Boolean emailSent,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false, defaultValue = "DESC") SortDirection sortDir) {
        return notificationService.filterAndSort(customerId, eventType, emailSent, q, sortBy, sortDir);
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE','OPERATOR')")
    @Operation(summary = "Search notifications", description = "Filter + sort. Staff only.")
    public List<NotificationResponse> search(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) Boolean emailSent,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false, defaultValue = "DESC") SortDirection sortDir) {
        return notificationService.search(customerId, eventType, emailSent, q, sortBy, sortDir);
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE','OPERATOR') or (hasRole('CUSTOMER') and @accessGuard.isOwnCustomer(#customerId))")
    @Operation(summary = "Notifications by customer", description = "Includes DB trigger messages.")
    public List<NotificationResponse> findByCustomer(
            @Parameter(description = "Customer id") @PathVariable Long customerId,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false, defaultValue = "DESC") SortDirection sortDir) {
        return notificationService.findByCustomer(customerId, sortBy, sortDir);
    }
}
