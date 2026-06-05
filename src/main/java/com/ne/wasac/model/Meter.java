package com.ne.wasac.model;

import com.ne.wasac.enums.MeterStatus;
import com.ne.wasac.enums.MeterType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Physical utility meter assigned to exactly one customer.
 */
@Getter
@Setter
@Entity
@Table(name = "meters")
public class Meter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique meter code WM-##### or EM-#####. */
    @Column(nullable = false, unique = true)
    private String meterNumber;

    /** WATER or ELECTRICITY. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MeterType meterType;

    /** Date the meter was installed. */
    @Column(nullable = false)
    private LocalDate installationDate;

    /** ACTIVE meters accept readings; DISCONNECTED blocks billing. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MeterStatus status = MeterStatus.ACTIVE;

    /** Owning customer — one customer, many meters. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "customer_id")
    private Customer customer;
}
