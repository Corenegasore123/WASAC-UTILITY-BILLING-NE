package com.ne.wasac.service;

import com.ne.wasac.dto.*;
import com.ne.wasac.model.*;

import java.util.List;

public final class DtoMapper {

    private DtoMapper() {
    }

    public static CustomerResponse toCustomerResponse(Customer c) {
        return CustomerResponse.builder()
                .id(c.getId())
                .fullName(c.getFullName())
                .nationalId(c.getNationalId())
                .email(c.getEmail())
                .phone(c.getPhone())
                .address(c.getAddress())
                .dateOfBirth(c.getDateOfBirth())
                .status(c.getStatus())
                .build();
    }

    public static MeterResponse toMeterResponse(Meter m) {
        return MeterResponse.builder()
                .id(m.getId())
                .meterNumber(m.getMeterNumber())
                .meterType(m.getMeterType())
                .installationDate(m.getInstallationDate())
                .status(m.getStatus())
                .customerId(m.getCustomer().getId())
                .customerName(m.getCustomer().getFullName())
                .build();
    }

    public static MeterReadingResponse toMeterReadingResponse(MeterReading r) {
        return MeterReadingResponse.builder()
                .id(r.getId())
                .meterId(r.getMeter().getId())
                .meterNumber(r.getMeter().getMeterNumber())
                .previousReading(r.getPreviousReading())
                .currentReading(r.getCurrentReading())
                .readingDate(r.getReadingDate())
                .billingMonth(r.getBillingMonth())
                .billingYear(r.getBillingYear())
                .build();
    }

    public static TariffPlanResponse toTariffPlanResponse(TariffPlan t) {
        List<TariffTierResponse> tiers = t.getTiers().stream()
                .map(tier -> TariffTierResponse.builder()
                        .id(tier.getId())
                        .minUnit(tier.getMinUnit())
                        .maxUnit(tier.getMaxUnit())
                        .ratePerUnit(tier.getRatePerUnit())
                        .build())
                .toList();
        return TariffPlanResponse.builder()
                .id(t.getId())
                .meterType(t.getMeterType())
                .tariffType(t.getTariffType())
                .versionNo(t.getVersionNo())
                .effectiveFrom(t.getEffectiveFrom())
                .effectiveTo(t.getEffectiveTo())
                .flatRatePerUnit(t.getFlatRatePerUnit())
                .fixedServiceCharge(t.getFixedServiceCharge())
                .vatRate(t.getVatRate())
                .latePenaltyRate(t.getLatePenaltyRate())
                .tiers(tiers)
                .build();
    }

    public static BillResponse toBillResponse(Bill b) {
        return BillResponse.builder()
                .id(b.getId())
                .billReference(b.getBillReference())
                .customerId(b.getCustomer().getId())
                .customerName(b.getCustomer().getFullName())
                .meterId(b.getMeter().getId())
                .meterNumber(b.getMeter().getMeterNumber())
                .billingMonth(b.getBillingMonth())
                .billingYear(b.getBillingYear())
                .consumption(b.getConsumption())
                .amountBeforeTax(b.getAmountBeforeTax())
                .taxAmount(b.getTaxAmount())
                .penaltyAmount(b.getPenaltyAmount())
                .totalAmount(b.getTotalAmount())
                .paidAmount(b.getPaidAmount())
                .outstandingBalance(b.getOutstandingBalance())
                .status(b.getStatus())
                .dueDate(b.getDueDate())
                .approvedBy(b.getApprovedBy() != null ? b.getApprovedBy().getFullName() : null)
                .approvedAt(b.getApprovedAt())
                .build();
    }

    public static PaymentResponse toPaymentResponse(Payment p) {
        return PaymentResponse.builder()
                .id(p.getId())
                .billId(p.getBill().getId())
                .billReference(p.getBill().getBillReference())
                .amountPaid(p.getAmountPaid())
                .paymentMethod(p.getPaymentMethod())
                .paymentDate(p.getPaymentDate())
                .build();
    }

    public static NotificationResponse toNotificationResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .customerId(n.getCustomer().getId())
                .message(n.getMessage())
                .eventType(n.getEventType())
                .emailSent(n.isEmailSent())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
