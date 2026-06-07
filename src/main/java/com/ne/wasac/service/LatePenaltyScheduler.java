package com.ne.wasac.service;

import com.ne.wasac.enums.BillStatus;
import com.ne.wasac.model.Bill;
import com.ne.wasac.model.TariffPlan;
import com.ne.wasac.repository.BillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Applies late-payment penalties automatically on a cron schedule.
 * Runs in the background; staff can also trigger via POST /api/bills/{id}/apply-penalty.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LatePenaltyScheduler {

    private final BillRepository billRepository;
    private final TariffService tariffService;
    private final BillService billService;

    @Value("${app.billing.penalty-days-overdue:30}")
    private int penaltyDaysOverdue;

    @Scheduled(cron = "${app.scheduler.cron.penalties:0 0 2 * * *}")
    public void applyLatePenalties() {
        LocalDate cutoff = LocalDate.now().minusDays(penaltyDaysOverdue);
        List<Bill> overdue = billRepository.findByStatusInAndDueDateBefore(
                List.of(BillStatus.UNPAID, BillStatus.APPROVED, BillStatus.PARTIALLY_PAID), cutoff);
        int count = 0;
        for (Bill bill : overdue) {
            try {
                TariffPlan tariff = tariffService.getApplicableTariff(
                        bill.getMeter().getMeterType(), bill.getBillingMonth(), bill.getBillingYear());
                billService.applyLatePenalty(bill, tariff);
                count++;
            } catch (Exception ex) {
                log.error("Penalty failed for bill {}: {}", bill.getId(), ex.getMessage());
            }
        }
        if (count > 0) {
            log.info("Late penalty job completed — processed {} bill(s)", count);
        }
    }
}
