package com.ne.wasac.config;

import com.ne.wasac.event.BillApprovedEvent;
import com.ne.wasac.event.BillGeneratedEvent;
import com.ne.wasac.model.Bill;
import com.ne.wasac.model.Notification;
import com.ne.wasac.repository.BillRepository;
import com.ne.wasac.repository.NotificationRepository;
import com.ne.wasac.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends bill lifecycle emails after commit so DB trigger notifications exist first.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BillEmailListener {

    private final BillRepository billRepository;
    private final NotificationRepository notificationRepository;
    private final EmailService emailService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBillGenerated(BillGeneratedEvent event) {
        billRepository.findByIdWithCustomerAndMeter(event.billId()).ifPresentOrElse(bill -> {
            Notification notification = notificationRepository
                    .findByCustomer_IdAndEventTypeAndReferenceId(
                            bill.getCustomer().getId(), "BILL_GENERATED", bill.getId())
                    .orElse(null);
            emailService.sendBillGenerated(bill.getCustomer(), bill, notification);
        }, () -> log.warn("Bill {} not found for generated email", event.billId()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBillApproved(BillApprovedEvent event) {
        billRepository.findByIdWithCustomerAndMeter(event.billId()).ifPresentOrElse(
                bill -> emailService.sendBillApproved(bill.getCustomer(), bill),
                () -> log.warn("Bill {} not found for approved email", event.billId()));
    }
}
