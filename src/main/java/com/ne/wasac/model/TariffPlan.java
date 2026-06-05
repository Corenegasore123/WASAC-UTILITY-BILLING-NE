package com.ne.wasac.model;

import com.ne.wasac.enums.MeterType;
import com.ne.wasac.enums.TariffType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "tariff_plans")
public class TariffPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MeterType meterType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TariffType tariffType;

    @Column(nullable = false)
    private Integer versionNo;

    @Column(nullable = false)
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    @Column(precision = 19, scale = 2)
    private BigDecimal flatRatePerUnit;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal fixedServiceCharge;

    @Column(nullable = false, precision = 8, scale = 4)
    private BigDecimal vatRate;

    @Column(nullable = false, precision = 8, scale = 4)
    private BigDecimal latePenaltyRate;

    @OneToMany(mappedBy = "tariffPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TariffTier> tiers = new ArrayList<>();
}
