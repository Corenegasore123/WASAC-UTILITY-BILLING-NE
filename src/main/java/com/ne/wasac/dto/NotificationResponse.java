package com.ne.wasac.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/** Notification row exposed to API consumers. */
@Getter
@Builder
public class NotificationResponse {
    private Long id;
    private Long customerId;
    private String message;
    private String eventType;
    private boolean emailSent;
    private LocalDateTime createdAt;
}
