package com.ne.wasac.dto;

import com.ne.wasac.enums.MeterType;
import com.ne.wasac.enums.TariffType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class TariffPlanResponse {
    private Long id;
    private MeterType meterType;
    private TariffType tariffType;
    private Integer versionNo;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private BigDecimal flatRatePerUnit;
    private BigDecimal fixedServiceCharge;
    private BigDecimal vatRate;
    private BigDecimal latePenaltyRate;
    private List<TariffTierResponse> tiers;
}
