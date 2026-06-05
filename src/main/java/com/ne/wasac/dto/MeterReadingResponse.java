package com.ne.wasac.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class MeterReadingResponse {
    private Long id;
    private Long meterId;
    private String meterNumber;
    private BigDecimal previousReading;
    private BigDecimal currentReading;
    private LocalDate readingDate;
    private Integer billingMonth;
    private Integer billingYear;
}
