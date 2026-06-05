package com.ne.wasac.dto;

import com.ne.wasac.enums.PaymentMethod;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class PaymentResponse {
    private Long id;
    private Long billId;
    private String billReference;
    private BigDecimal amountPaid;
    private PaymentMethod paymentMethod;
    private LocalDate paymentDate;
}
