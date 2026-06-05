package com.ne.wasac.repository;

import com.ne.wasac.enums.MeterType;
import com.ne.wasac.enums.TariffType;
import com.ne.wasac.model.TariffPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TariffPlanRepository extends JpaRepository<TariffPlan, Long> {

    List<TariffPlan> findByMeterTypeOrderByVersionNoDesc(MeterType meterType);

    @Query("""
            SELECT t FROM TariffPlan t
            WHERE t.meterType = :meterType
              AND t.effectiveFrom <= :cycleEnd
              AND (t.effectiveTo IS NULL OR t.effectiveTo >= :cycleStart)
            ORDER BY t.versionNo DESC
            LIMIT 1
            """)
    Optional<TariffPlan> findApplicableTariff(@Param("meterType") MeterType meterType,
                                            @Param("cycleStart") LocalDate cycleStart,
                                            @Param("cycleEnd") LocalDate cycleEnd);

    @Query("""
            SELECT t FROM TariffPlan t
            WHERE (:meterType IS NULL OR t.meterType = :meterType)
              AND (:tariffType IS NULL OR t.tariffType = :tariffType)
              AND (:effectiveFrom IS NULL OR t.effectiveFrom >= :effectiveFrom)
              AND (:effectiveTo IS NULL OR t.effectiveFrom <= :effectiveTo)
            """)
    List<TariffPlan> search(
            @Param("meterType") MeterType meterType,
            @Param("tariffType") TariffType tariffType,
            @Param("effectiveFrom") LocalDate effectiveFrom,
            @Param("effectiveTo") LocalDate effectiveTo);
}
