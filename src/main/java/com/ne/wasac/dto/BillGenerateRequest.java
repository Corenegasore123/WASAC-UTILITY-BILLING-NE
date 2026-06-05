package com.ne.wasac.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BillGenerateRequest {
    @NotNull
    private Long meterReadingId;

    @NotNull
    @Min(1)
    @Max(12)
    private Integer billingMonth;

    @NotNull
    @Min(2000)
    private Integer billingYear;

    private boolean applyPenalty;
}
