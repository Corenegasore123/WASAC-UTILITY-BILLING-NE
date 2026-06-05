package com.ne.wasac.dto;

import com.ne.wasac.enums.MeterStatus;
import com.ne.wasac.enums.MeterType;
import com.ne.wasac.validation.MeterNumberFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Request to register or update a utility meter on a customer account.
 */
@Getter
@Setter
public class MeterRequest {

    @NotBlank
    @MeterNumberFormat // WM-##### or EM-#####
    private String meterNumber;

    @NotNull(message = "Meter type must be WATER or ELECTRICITY")
    private MeterType meterType;

    @NotNull
    @PastOrPresent(message = "Installation date cannot be in the future")
    private LocalDate installationDate;

    @NotNull
    private MeterStatus status;

    @NotNull(message = "Customer id is required")
    private Long customerId;
}
