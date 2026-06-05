package com.ne.wasac.repository;

import com.ne.wasac.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    Optional<Notification> findByCustomer_IdAndEventTypeAndReferenceId(
            Long customerId, String eventType, Long referenceId);

    @Query("""
            SELECT n FROM Notification n
            WHERE (:customerId IS NULL OR n.customer.id = :customerId)
              AND (:eventType IS NULL OR LOWER(n.eventType) = LOWER(:eventType))
              AND (:emailSent IS NULL OR n.emailSent = :emailSent)
              AND (:q IS NULL OR LOWER(n.message) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(n.eventType) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    List<Notification> search(
            @Param("customerId") Long customerId,
            @Param("eventType") String eventType,
            @Param("emailSent") Boolean emailSent,
            @Param("q") String q);
}
