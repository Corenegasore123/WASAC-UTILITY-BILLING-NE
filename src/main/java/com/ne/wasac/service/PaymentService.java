package com.ne.wasac.service;

import com.ne.wasac.dto.PaymentRequest;
import com.ne.wasac.dto.PaymentResponse;
import com.ne.wasac.enums.AuditAction;
import com.ne.wasac.enums.BillStatus;
import com.ne.wasac.enums.PaymentMethod;
import com.ne.wasac.enums.SortDirection;
import com.ne.wasac.util.QuerySort;
import com.ne.wasac.exception.BusinessRuleException;
import com.ne.wasac.model.Bill;
import com.ne.wasac.model.Payment;
import com.ne.wasac.repository.BillRepository;
import com.ne.wasac.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Records partial and full payments; updates bill balances and status.
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final Map<String, Comparator<PaymentResponse>> SORT_FIELDS = Map.of(
            "id", Comparator.comparing(PaymentResponse::getId),
            "amountPaid", Comparator.comparing(PaymentResponse::getAmountPaid),
            "paymentDate", Comparator.comparing(PaymentResponse::getPaymentDate),
            "paymentMethod", Comparator.comparing(p -> p.getPaymentMethod().name()),
            "billReference", Comparator.comparing(PaymentResponse::getBillReference, String.CASE_INSENSITIVE_ORDER));

    private final PaymentRepository paymentRepository;
    private final BillRepository billRepository;
    private final BillService billService;
    private final EmailService emailService;
    private final AuditService auditService;

    /**
     * Records payment after validating bill reference, approval status, and amount.
     */
    @Transactional
    public PaymentResponse recordPayment(PaymentRequest request) {
        Bill bill = billService.getBill(request.getBillId());
        if (!bill.getBillReference().equals(request.getBillReference())) {
            throw new BusinessRuleException("Bill reference does not match bill id");
        }
        if (bill.getStatus() == BillStatus.UNPAID) {
            throw new BusinessRuleException("Bill must be approved before accepting payments");
        }
        if (bill.getStatus() == BillStatus.PAID) {
            throw new BusinessRuleException("Bill is already fully paid");
        }
        if (request.getAmountPaid().compareTo(bill.getOutstandingBalance()) > 0) {
            throw new BusinessRuleException("Payment amount exceeds outstanding balance");
        }

        BigDecimal newPaid = bill.getPaidAmount().add(request.getAmountPaid());
        BigDecimal newOutstanding = bill.getTotalAmount().subtract(newPaid);
        bill.setPaidAmount(newPaid);
        bill.setOutstandingBalance(newOutstanding);

        if (newOutstanding.compareTo(BigDecimal.ZERO) == 0) {
            bill.setStatus(BillStatus.PAID);
        } else {
            bill.setStatus(BillStatus.PARTIALLY_PAID);
        }

        Payment payment = new Payment();
        payment.setBill(bill);
        payment.setAmountPaid(request.getAmountPaid());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setPaymentDate(request.getPaymentDate());

        billRepository.save(bill);
        Payment saved = paymentRepository.save(payment);
        paymentRepository.flush();
        auditService.log(AuditAction.PAYMENT_RECORDED, "Payment", saved.getId(), null,
                request.getAmountPaid().toPlainString());

        if (bill.getStatus() == BillStatus.PAID) {
            emailService.sendBillFullyPaid(bill.getCustomer(), bill);
        } else {
            emailService.sendPaymentReceived(bill.getCustomer(), bill, request.getAmountPaid());
        }
        return DtoMapper.toPaymentResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> findByBill(Long billId) {
        return paymentRepository.findByBillIdOrderByPaymentDateDesc(billId)
                .stream().map(DtoMapper::toPaymentResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> findByCustomer(Long customerId) {
        return paymentRepository.findByCustomerId(customerId)
                .stream().map(DtoMapper::toPaymentResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> search(String q, Long customerId, Long billId,
                                        PaymentMethod method, LocalDate from, LocalDate to,
                                        String sortBy, SortDirection sortDir) {
        return sort(paymentRepository.search(blankToNull(q), customerId, billId, method, from, to)
                .stream().map(DtoMapper::toPaymentResponse).toList(), sortBy, sortDir);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> filterAndSort(String q, Long customerId, Long billId,
                                               PaymentMethod method, LocalDate from, LocalDate to,
                                               String sortBy, SortDirection sortDir) {
        if (blankToNull(q) != null || customerId != null || billId != null || method != null
                || from != null || to != null) {
            return search(q, customerId, billId, method, from, to, sortBy, sortDir);
        }
        return sort(paymentRepository.findAll().stream().map(DtoMapper::toPaymentResponse).toList(),
                sortBy, sortDir);
    }

    private List<PaymentResponse> sort(List<PaymentResponse> items, String sortBy, SortDirection sortDir) {
        return QuerySort.apply(items, sortBy, sortDir == null ? SortDirection.DESC : sortDir,
                SORT_FIELDS, "paymentDate");
    }

    private String blankToNull(String value) {
        return QuerySort.blankToNull(value);
    }
}
