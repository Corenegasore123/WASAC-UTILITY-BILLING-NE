package com.ne.wasac.dto;

import com.ne.wasac.enums.MeterStatus;
import com.ne.wasac.enums.MeterType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

/** Meter details including owning customer summary. */
@Getter
@Builder
public class MeterResponse {
    private Long id;
    private String meterNumber;
    private MeterType meterType;
    private LocalDate installationDate;
    private MeterStatus status;
    private Long customerId;
    private String customerName;
}
