package com.ne.wasac.dto;

import com.ne.wasac.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payment against an approved bill (partial or full).
 */
@Getter
@Setter
public class PaymentRequest {

    @NotNull(message = "Bill id is required")
    private Long billId;

    @NotBlank(message = "Bill reference is required")
    private String billReference;

    @NotNull
    @DecimalMin(value = "1.0", message = "Amount paid must be greater than 0")
    private BigDecimal amountPaid;

    @NotNull(message = "Payment method must be MOMO, BANK, CARD, or CASH")
    private PaymentMethod paymentMethod;

    @NotNull
    @PastOrPresent(message = "Payment date cannot be in the future")
    private LocalDate paymentDate;
}
