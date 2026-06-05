package com.ne.wasac.dto;

import com.ne.wasac.enums.BillStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class BillResponse {
    private Long id;
    private String billReference;
    private Long customerId;
    private String customerName;
    private Long meterId;
    private String meterNumber;
    private Integer billingMonth;
    private Integer billingYear;
    private BigDecimal consumption;
    private BigDecimal amountBeforeTax;
    private BigDecimal taxAmount;
    private BigDecimal penaltyAmount;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal outstandingBalance;
    private BillStatus status;
    private LocalDate dueDate;
    private String approvedBy;
    private LocalDateTime approvedAt;
}
