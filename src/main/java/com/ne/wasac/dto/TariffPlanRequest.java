package com.ne.wasac.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.ne.wasac.enums.MeterType;
import com.ne.wasac.enums.TariffType;
import com.ne.wasac.json.LenientBigDecimalDeserializer;
import com.ne.wasac.json.LenientLocalDateDeserializer;
import com.ne.wasac.validation.ValidTariffTiers;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Admin tariff configuration with optional tier blocks.
 */
@Getter
@Setter
@ValidTariffTiers // Tier ranges continuous and non-overlapping when TIERED
public class TariffPlanRequest {

    @NotNull
    private MeterType meterType;

    @NotNull
    private TariffType tariffType;

    @NotNull
    @FutureOrPresent(message = "Effective date must be today or a future date")
    private LocalDate effectiveFrom;

    @JsonDeserialize(using = LenientLocalDateDeserializer.class)
    private LocalDate effectiveTo;

    @DecimalMin(value = "1.0", message = "Unit price must be greater than 0")
    @JsonDeserialize(using = LenientBigDecimalDeserializer.class)
    private BigDecimal flatRatePerUnit;

    @NotNull
    @DecimalMin(value = "0.0", message = "Service charge cannot be negative")
    private BigDecimal fixedServiceCharge;

    @NotNull
    @DecimalMin("0.0")
    @DecimalMax(value = "100.0", message = "VAT cannot exceed 100%")
    private BigDecimal vatRate;

    @NotNull
    @DecimalMin(value = "0.0", message = "Penalty rate cannot be negative")
    private BigDecimal latePenaltyRate;

    @Valid
    private List<TariffTierRequest> tiers;
}
