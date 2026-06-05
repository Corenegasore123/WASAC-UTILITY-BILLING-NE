package com.ne.wasac.dto;

import com.ne.wasac.validation.ValidMeterReading;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Operator capture of a monthly meter reading.
 */
@Getter
@Setter
@ValidMeterReading // current must be > previous
public class MeterReadingRequest {

    @NotNull(message = "Meter id is required")
    private Long meterId;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true, message = "Previous reading cannot be negative")
    private BigDecimal previousReading;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true, message = "Current reading cannot be negative")
    private BigDecimal currentReading;

    @NotNull
    @PastOrPresent(message = "Reading date cannot be in the future")
    private LocalDate readingDate;

    @NotNull
    @Min(1)
    @Max(12)
    private Integer billingMonth;

    @NotNull
    @Min(2000)
    private Integer billingYear;
}
