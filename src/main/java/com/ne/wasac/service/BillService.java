package com.ne.wasac.service;

import com.ne.wasac.dto.BillGenerateRequest;
import com.ne.wasac.dto.BillResponse;
import com.ne.wasac.enums.AuditAction;
import com.ne.wasac.enums.BillStatus;
import com.ne.wasac.enums.SortDirection;
import com.ne.wasac.enums.Status;
import com.ne.wasac.enums.TariffType;
import com.ne.wasac.util.QuerySort;
import com.ne.wasac.exception.BusinessRuleException;
import com.ne.wasac.exception.ResourceNotFoundException;
import com.ne.wasac.model.*;
import com.ne.wasac.repository.BillRepository;
import com.ne.wasac.security.SecurityUser;
import com.ne.wasac.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Generates monthly bills from readings and manages finance approval workflow.
 */
@Service
@RequiredArgsConstructor
public class BillService {

    private static final Map<String, Comparator<BillResponse>> SORT_FIELDS = Map.of(
            "id", Comparator.comparing(BillResponse::getId),
            "billReference", Comparator.comparing(BillResponse::getBillReference, String.CASE_INSENSITIVE_ORDER),
            "totalAmount", Comparator.comparing(BillResponse::getTotalAmount),
            "outstandingBalance", Comparator.comparing(BillResponse::getOutstandingBalance),
            "dueDate", Comparator.comparing(BillResponse::getDueDate),
            "billingYear", Comparator.comparing(BillResponse::getBillingYear),
            "billingMonth", Comparator.comparing(BillResponse::getBillingMonth),
            "status", Comparator.comparing(b -> b.getStatus().name()));

    private final BillRepository billRepository;
    private final MeterReadingService meterReadingService;
    private final TariffService tariffService;
    private final EmailService emailService;
    private final AuditService auditService;

    /**
     * Creates a bill from a meter reading. Enforces one bill per meter/period
     * and rejects inactive customers.
     */
    @Transactional
    public BillResponse generate(BillGenerateRequest request) {
        MeterReading reading = meterReadingService.getReading(request.getMeterReadingId());
        Meter meter = reading.getMeter();
        Customer customer = meter.getCustomer();

        if (customer.getStatus() != Status.ACTIVE) {
            throw new BusinessRuleException("Cannot generate bills for inactive customers");
        }
        if (!reading.getBillingMonth().equals(request.getBillingMonth())
                || !reading.getBillingYear().equals(request.getBillingYear())) {
            throw new BusinessRuleException("Billing period does not match meter reading");
        }
        if (billRepository.existsByMeterIdAndBillingMonthAndBillingYear(
                meter.getId(), request.getBillingMonth(), request.getBillingYear())) {
            throw new BusinessRuleException("Bill already exists for this meter and billing period");
        }

        BigDecimal consumption = reading.getCurrentReading().subtract(reading.getPreviousReading());
        if (consumption.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Units consumed must be greater than 0");
        }

        TariffPlan tariff = tariffService.getApplicableTariff(
                meter.getMeterType(), request.getBillingMonth(), request.getBillingYear());

        BigDecimal usageCharge = calculateUsageCharge(tariff, consumption);
        BigDecimal amountBeforeTax = usageCharge.add(tariff.getFixedServiceCharge());
        BigDecimal taxAmount = amountBeforeTax.multiply(tariff.getVatRate())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        BigDecimal penaltyAmount = BigDecimal.ZERO;
        if (request.isApplyPenalty()) {
            penaltyAmount = amountBeforeTax.multiply(tariff.getLatePenaltyRate())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }

        BigDecimal totalAmount = amountBeforeTax.add(taxAmount).add(penaltyAmount);
        LocalDate dueDate = LocalDate.now().plusDays(30);
        if (!dueDate.isAfter(LocalDate.now())) {
            throw new BusinessRuleException("Due date must be in the future");
        }

        Bill bill = new Bill();
        bill.setBillReference("BILL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        bill.setCustomer(customer);
        bill.setMeter(meter);
        bill.setMeterReading(reading);
        bill.setBillingMonth(request.getBillingMonth());
        bill.setBillingYear(request.getBillingYear());
        bill.setConsumption(consumption);
        bill.setAmountBeforeTax(amountBeforeTax);
        bill.setTaxAmount(taxAmount);
        bill.setPenaltyAmount(penaltyAmount);
        bill.setTotalAmount(totalAmount);
        bill.setPaidAmount(BigDecimal.ZERO);
        bill.setOutstandingBalance(totalAmount);
        bill.setStatus(BillStatus.UNPAID);
        bill.setDueDate(dueDate);

        Bill saved = billRepository.save(bill);
        billRepository.flush();
        emailService.sendBillGenerated(customer, saved, null);
        return DtoMapper.toBillResponse(saved);
    }

    /**
     * Finance approval: UNPAID → APPROVED. Only pending bills may be approved.
     */
    @Transactional
    public BillResponse approve(Long billId) {
        Bill bill = getBill(billId);
        if (bill.getStatus() != BillStatus.UNPAID) {
            throw new BusinessRuleException("Only unpaid bills awaiting approval can be approved");
        }
        BillStatus old = bill.getStatus();
        SecurityUser user = SecurityUtils.currentUser();
        bill.setStatus(BillStatus.APPROVED);
        bill.setApprovedBy(user.getUser());
        bill.setApprovedAt(LocalDateTime.now());
        Bill saved = billRepository.save(bill);
        auditService.log(AuditAction.BILL_APPROVED, "Bill", saved.getId(), old.name(), BillStatus.APPROVED.name());
        emailService.sendBillApproved(saved.getCustomer(), saved);
        return DtoMapper.toBillResponse(saved);
    }

    /**
     * Staff-triggered late penalty for a single overdue bill.
     * Requires the due date to have passed; uses the tariff latePenaltyRate.
     */
    @Transactional
    public BillResponse applyLatePenalty(Long billId) {
        Bill bill = getBill(billId);
        if (bill.getStatus() == BillStatus.PAID) {
            throw new BusinessRuleException("Cannot apply penalty to a fully paid bill");
        }
        if (!bill.getDueDate().isBefore(LocalDate.now())) {
            throw new BusinessRuleException("Bill is not overdue yet");
        }
        TariffPlan tariff = tariffService.getApplicableTariff(
                bill.getMeter().getMeterType(), bill.getBillingMonth(), bill.getBillingYear());
        BigDecimal previousPenalty = bill.getPenaltyAmount();
        applyLatePenalty(bill, tariff);
        auditService.log(AuditAction.BILL_PENALTY_APPLIED, "Bill", billId,
                previousPenalty.toPlainString(), bill.getPenaltyAmount().toPlainString());
        return DtoMapper.toBillResponse(bill);
    }

    /** Applies a late-payment penalty to an overdue bill (used by cron and manual API). */
    @Transactional
    public void applyLatePenalty(Bill bill, TariffPlan tariff) {
        if (bill.getStatus() == BillStatus.PAID) {
            return;
        }
        BigDecimal extra = bill.getAmountBeforeTax().multiply(tariff.getLatePenaltyRate())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        bill.setPenaltyAmount(bill.getPenaltyAmount().add(extra));
        bill.setTotalAmount(bill.getTotalAmount().add(extra));
        bill.setOutstandingBalance(bill.getOutstandingBalance().add(extra));
        billRepository.save(bill);
        emailService.sendLatePaymentWarning(bill.getCustomer(), bill);
    }

    @Transactional(readOnly = true)
    public List<BillResponse> findAll(String sortBy, SortDirection sortDir) {
        return sort(billRepository.findAll().stream().map(DtoMapper::toBillResponse).toList(), sortBy, sortDir);
    }

    @Transactional(readOnly = true)
    public BillResponse findById(Long id) {
        return DtoMapper.toBillResponse(getBill(id));
    }

    @Transactional(readOnly = true)
    public List<BillResponse> findByCustomer(Long customerId) {
        return billRepository.findByCustomerIdOrderByBillingYearDescBillingMonthDesc(customerId)
                .stream().map(DtoMapper::toBillResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<BillResponse> search(String q, BillStatus status, Long customerId, String reference,
                                     String sortBy, SortDirection sortDir) {
        return sort(billRepository.search(blankToNull(q), status, customerId, blankToNull(reference))
                .stream().map(DtoMapper::toBillResponse).toList(), sortBy, sortDir);
    }

    @Transactional(readOnly = true)
    public List<BillResponse> filterAndSort(String q, BillStatus status, Long customerId, String reference,
                                            String sortBy, SortDirection sortDir) {
        if (blankToNull(q) != null || status != null || customerId != null || blankToNull(reference) != null) {
            return search(q, status, customerId, reference, sortBy, sortDir);
        }
        return findAll(sortBy, sortDir);
    }

    private List<BillResponse> sort(List<BillResponse> items, String sortBy, SortDirection sortDir) {
        return QuerySort.apply(items, sortBy, sortDir == null ? SortDirection.DESC : sortDir,
                SORT_FIELDS, "billingYear");
    }

    /** Loads a bill or throws 404. */
    public Bill getBill(Long id) {
        return billRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found: " + id));
    }

    /** Computes usage charge for FLAT or TIERED tariff. */
    private BigDecimal calculateUsageCharge(TariffPlan tariff, BigDecimal consumption) {
        if (tariff.getTariffType() == TariffType.FLAT) {
            return consumption.multiply(tariff.getFlatRatePerUnit()).setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal remaining = consumption;
        BigDecimal charge = BigDecimal.ZERO;
        List<TariffTier> tiers = tariff.getTiers().stream()
                .sorted(Comparator.comparing(TariffTier::getMinUnit))
                .toList();
        for (TariffTier tier : tiers) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal tierWidth = tier.getMaxUnit().subtract(tier.getMinUnit());
            BigDecimal unitsInTier = remaining.min(tierWidth);
            if (unitsInTier.compareTo(BigDecimal.ZERO) > 0) {
                charge = charge.add(unitsInTier.multiply(tier.getRatePerUnit()));
                remaining = remaining.subtract(unitsInTier);
            }
        }
        if (remaining.compareTo(BigDecimal.ZERO) > 0 && !tiers.isEmpty()) {
            TariffTier last = tiers.getLast();
            charge = charge.add(remaining.multiply(last.getRatePerUnit()));
        }
        return charge.setScale(2, RoundingMode.HALF_UP);
    }

    private String blankToNull(String value) {
        return QuerySort.blankToNull(value);
    }
}
