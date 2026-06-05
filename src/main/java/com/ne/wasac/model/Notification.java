package com.ne.wasac.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Customer notification created by DB triggers or application events.
 * emailSent tracks whether JavaMailSender delivered the message.
 */
@Getter
@Setter
@Entity
@Table(name = "notifications",
        uniqueConstraints = @UniqueConstraint(name = "uk_notification_event",
                columnNames = {"customer_id", "event_type", "reference_id"}))
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Recipient customer. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    /** Human-readable notification body. */
    @Column(nullable = false, length = 500)
    private String message;

    /** Event key used to prevent duplicate notifications e.g. BILL_GENERATED. */
    @Column(name = "event_type", nullable = false)
    private String eventType;

    /** Related entity id (bill id, payment id, etc.). */
    @Column(name = "reference_id")
    private Long referenceId;

    /** True after EmailService successfully sends the message. */
    @Column(name = "email_sent", nullable = false)
    private boolean emailSent = false;

    /** Creation timestamp. */
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
