package com.ne.wasac.repository;

import com.ne.wasac.model.MeterReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MeterReadingRepository extends JpaRepository<MeterReading, Long> {

    Optional<MeterReading> findByMeterIdAndBillingMonthAndBillingYear(Long meterId, Integer month, Integer year);

    List<MeterReading> findByMeterIdOrderByBillingYearDescBillingMonthDesc(Long meterId);

    Optional<MeterReading> findTopByMeterIdOrderByBillingYearDescBillingMonthDesc(Long meterId);

    boolean existsByMeterIdAndBillingMonthAndBillingYear(Long meterId, Integer month, Integer year);

    @Query("""
            SELECT r FROM MeterReading r
            WHERE (:meterId IS NULL OR r.meter.id = :meterId)
              AND (:billingMonth IS NULL OR r.billingMonth = :billingMonth)
              AND (:billingYear IS NULL OR r.billingYear = :billingYear)
              AND (:q IS NULL OR LOWER(r.meter.meterNumber) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    List<MeterReading> search(
            @Param("q") String q,
            @Param("meterId") Long meterId,
            @Param("billingMonth") Integer billingMonth,
            @Param("billingYear") Integer billingYear);
}
