package com.ne.wasac.repository;

import com.ne.wasac.model.Meter;
import com.ne.wasac.enums.MeterStatus;
import com.ne.wasac.enums.MeterType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MeterRepository extends JpaRepository<Meter, Long> {
    Optional<Meter> findByMeterNumber(String meterNumber);
    List<Meter> findByCustomerId(Long customerId);
    boolean existsByMeterNumber(String meterNumber);

    @Query("""
            SELECT m FROM Meter m
            WHERE (:q IS NULL OR LOWER(m.meterNumber) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(m.customer.fullName) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:status IS NULL OR m.status = :status)
              AND (:meterType IS NULL OR m.meterType = :meterType)
              AND (:customerId IS NULL OR m.customer.id = :customerId)
            """)
    List<Meter> search(
            @Param("q") String q,
            @Param("status") MeterStatus status,
            @Param("meterType") MeterType meterType,
            @Param("customerId") Long customerId);
}
