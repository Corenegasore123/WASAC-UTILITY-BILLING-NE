package com.ne.wasac.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "meter_readings",
        uniqueConstraints = @UniqueConstraint(name = "uk_meter_month_year", columnNames = {"meter_id", "billing_month", "billing_year"}))
public class MeterReading {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "meter_id")
    private Meter meter;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal previousReading;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal currentReading;

    @Column(nullable = false)
    private LocalDate readingDate;

    @Column(name = "billing_month", nullable = false)
    private Integer billingMonth;

    @Column(name = "billing_year", nullable = false)
    private Integer billingYear;
}
