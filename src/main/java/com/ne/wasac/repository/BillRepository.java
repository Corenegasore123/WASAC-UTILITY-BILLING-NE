package com.ne.wasac.repository;

import com.ne.wasac.enums.BillStatus;
import com.ne.wasac.model.Bill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BillRepository extends JpaRepository<Bill, Long> {

    Optional<Bill> findByBillReference(String billReference);

    List<Bill> findByCustomerIdOrderByBillingYearDescBillingMonthDesc(Long customerId);

    boolean existsByMeterIdAndBillingMonthAndBillingYear(Long meterId, Integer month, Integer year);

    /** Finds unpaid bills past due date for penalty and disconnection jobs. */
    List<Bill> findByStatusInAndDueDateBefore(Collection<BillStatus> statuses, LocalDate dueDate);

    /** Bills due within a date window — used for payment reminder emails. */
    List<Bill> findByStatusInAndDueDateBetween(Collection<BillStatus> statuses, LocalDate from, LocalDate to);

    @Query("""
            SELECT b FROM Bill b
            WHERE (:status IS NULL OR b.status = :status)
              AND (:customerId IS NULL OR b.customer.id = :customerId)
              AND (:reference IS NULL OR LOWER(b.billReference) LIKE LOWER(CONCAT('%', :reference, '%')))
              AND (:q IS NULL OR LOWER(b.customer.fullName) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(b.billReference) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(b.meter.meterNumber) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    List<Bill> search(
            @Param("q") String q,
            @Param("status") BillStatus status,
            @Param("customerId") Long customerId,
            @Param("reference") String reference);
}
