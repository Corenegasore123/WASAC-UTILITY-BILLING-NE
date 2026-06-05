package com.ne.wasac.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "tariff_tiers")
public class TariffTier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tariff_plan_id")
    private TariffPlan tariffPlan;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal minUnit;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal maxUnit;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal ratePerUnit;
}
