package com.ne.wasac.repository;

import com.ne.wasac.model.Payment;
import com.ne.wasac.enums.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByBillIdOrderByPaymentDateDesc(Long billId);

    @Query("""
            SELECT p FROM Payment p
            WHERE p.bill.customer.id = :customerId
            ORDER BY p.paymentDate DESC
            """)
    List<Payment> findByCustomerId(@Param("customerId") Long customerId);

    @Query("""
            SELECT p FROM Payment p
            WHERE (:customerId IS NULL OR p.bill.customer.id = :customerId)
              AND (:billId IS NULL OR p.bill.id = :billId)
              AND (:method IS NULL OR p.paymentMethod = :method)
              AND (:from IS NULL OR p.paymentDate >= :from)
              AND (:to IS NULL OR p.paymentDate <= :to)
              AND (:q IS NULL OR LOWER(p.bill.billReference) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(p.bill.customer.fullName) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    List<Payment> search(
            @Param("q") String q,
            @Param("customerId") Long customerId,
            @Param("billId") Long billId,
            @Param("method") PaymentMethod method,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
