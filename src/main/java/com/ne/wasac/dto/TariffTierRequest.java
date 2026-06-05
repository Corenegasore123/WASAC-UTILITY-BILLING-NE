package com.ne.wasac.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Single consumption tier for a TIERED tariff plan.
 */
@Getter
@Setter
public class TariffTierRequest {

    @NotNull
    @DecimalMin(value = "0.0", message = "min_units cannot be negative")
    private BigDecimal minUnit;

    @NotNull
    private BigDecimal maxUnit;

    @NotNull
    @DecimalMin(value = "1.0", message = "price_per_unit must be greater than 0")
    private BigDecimal ratePerUnit;
}
