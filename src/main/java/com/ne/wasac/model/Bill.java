package com.ne.wasac.model;

import com.ne.wasac.enums.BillStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Monthly utility bill generated from a meter reading and tariff.
 */
@Getter
@Setter
@Entity
@Table(name = "bills",
        uniqueConstraints = @UniqueConstraint(name = "uk_bill_meter_period",
                columnNames = {"meter_id", "billing_month", "billing_year"}))
public class Bill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Public bill reference for payments. */
    @Column(nullable = false, unique = true)
    private String billReference;

    @ManyToOne(optional = false)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(optional = false)
    @JoinColumn(name = "meter_id")
    private Meter meter;

    /** Source reading used to compute this bill. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "meter_reading_id")
    private MeterReading meterReading;

    @Column(nullable = false)
    private Integer billingMonth;

    @Column(nullable = false)
    private Integer billingYear;

    /** Units consumed (current - previous). */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal consumption;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amountBeforeTax;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal taxAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal penaltyAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal outstandingBalance;

    /** UNPAID → APPROVED → PARTIALLY_PAID / PAID. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BillStatus status = BillStatus.UNPAID;

    /** Payment deadline — must be in the future at creation. */
    @Column(nullable = false)
    private LocalDate dueDate;

    @ManyToOne
    @JoinColumn(name = "approved_by")
    private AppUser approvedBy;

    private LocalDateTime approvedAt;
}
