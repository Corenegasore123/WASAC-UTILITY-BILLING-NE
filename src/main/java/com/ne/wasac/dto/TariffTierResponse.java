package com.ne.wasac.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class TariffTierResponse {
    private Long id;
    private BigDecimal minUnit;
    private BigDecimal maxUnit;
    private BigDecimal ratePerUnit;
}
