package com.ne.wasac.service;

import com.ne.wasac.dto.TariffPlanRequest;
import com.ne.wasac.dto.TariffPlanResponse;
import com.ne.wasac.dto.TariffTierRequest;
import com.ne.wasac.enums.AuditAction;
import com.ne.wasac.enums.MeterType;
import com.ne.wasac.enums.SortDirection;
import com.ne.wasac.enums.TariffType;
import com.ne.wasac.util.QuerySort;
import com.ne.wasac.exception.BusinessRuleException;
import com.ne.wasac.exception.ResourceNotFoundException;
import com.ne.wasac.model.TariffPlan;
import com.ne.wasac.model.TariffTier;
import com.ne.wasac.repository.TariffPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Versioned tariff management. New tariffs apply only to future billing cycles.
 */
@Service
@RequiredArgsConstructor
public class TariffService {

    private static final Map<String, Comparator<TariffPlanResponse>> SORT_FIELDS = Map.of(
            "id", Comparator.comparing(TariffPlanResponse::getId),
            "versionNo", Comparator.comparing(TariffPlanResponse::getVersionNo),
            "meterType", Comparator.comparing(t -> t.getMeterType().name()),
            "tariffType", Comparator.comparing(t -> t.getTariffType().name()),
            "effectiveFrom", Comparator.comparing(TariffPlanResponse::getEffectiveFrom),
            "effectiveTo", Comparator.comparing(TariffPlanResponse::getEffectiveTo, Comparator.nullsLast(Comparator.naturalOrder())));

    private final TariffPlanRepository tariffPlanRepository;
    private final AuditService auditService;

    /**
     * Creates a new tariff version. Closes prior open versions for the meter type.
     */
    @Transactional
    public TariffPlanResponse create(TariffPlanRequest request) {
        validateTariffRequest(request);
        if (request.getEffectiveFrom().isBefore(LocalDate.now())) {
            throw new BusinessRuleException("Effective date must be today or a future date");
        }
        TariffPlan plan = new TariffPlan();
        plan.setMeterType(request.getMeterType());
        plan.setTariffType(request.getTariffType());
        plan.setEffectiveFrom(request.getEffectiveFrom());
        plan.setEffectiveTo(request.getEffectiveTo());
        plan.setFlatRatePerUnit(request.getFlatRatePerUnit());
        plan.setFixedServiceCharge(request.getFixedServiceCharge());
        plan.setVatRate(request.getVatRate());
        plan.setLatePenaltyRate(request.getLatePenaltyRate());
        int versionNo = nextVersion(request.getMeterType());
        plan.setVersionNo(versionNo);
        closePreviousVersions(request.getMeterType(), request.getEffectiveFrom());
        mapTiers(plan, request.getTiers());
        TariffPlan saved = tariffPlanRepository.save(plan);
        if (versionNo == 1) {
            auditService.log(AuditAction.TARIFF_CREATED, "TariffPlan", saved.getId(), null, saved.getVersionNo().toString());
        } else {
            auditService.log(AuditAction.TARIFF_UPDATED, "TariffPlan", saved.getId(),
                    String.valueOf(versionNo - 1), saved.getVersionNo().toString());
        }
        return DtoMapper.toTariffPlanResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<TariffPlanResponse> findAll(String sortBy, SortDirection sortDir) {
        return sort(tariffPlanRepository.findAll().stream().map(DtoMapper::toTariffPlanResponse).toList(),
                sortBy, sortDir);
    }

    @Transactional(readOnly = true)
    public List<TariffPlanResponse> search(MeterType meterType, TariffType tariffType,
                                           LocalDate effectiveFrom, LocalDate effectiveTo,
                                           String sortBy, SortDirection sortDir) {
        return sort(tariffPlanRepository.search(meterType, tariffType, effectiveFrom, effectiveTo)
                .stream().map(DtoMapper::toTariffPlanResponse).toList(), sortBy, sortDir);
    }

    @Transactional(readOnly = true)
    public List<TariffPlanResponse> filterAndSort(MeterType meterType, TariffType tariffType,
                                                  LocalDate effectiveFrom, LocalDate effectiveTo,
                                                  String sortBy, SortDirection sortDir) {
        if (meterType != null || tariffType != null || effectiveFrom != null || effectiveTo != null) {
            return search(meterType, tariffType, effectiveFrom, effectiveTo, sortBy, sortDir);
        }
        return findAll(sortBy, sortDir);
    }

    private List<TariffPlanResponse> sort(List<TariffPlanResponse> items, String sortBy, SortDirection sortDir) {
        return QuerySort.apply(items, sortBy, sortDir == null ? SortDirection.DESC : sortDir,
                SORT_FIELDS, "versionNo");
    }

    @Transactional(readOnly = true)
    public TariffPlanResponse findById(Long id) {
        return DtoMapper.toTariffPlanResponse(getTariff(id));
    }

    @Transactional(readOnly = true)
    public List<TariffPlanResponse> findByMeterType(MeterType meterType) {
        return tariffPlanRepository.findByMeterTypeOrderByVersionNoDesc(meterType)
                .stream().map(DtoMapper::toTariffPlanResponse).toList();
    }

    /** Resolves the tariff that overlaps the billing month (handles mid-month effectiveFrom). */
    public TariffPlan getApplicableTariff(MeterType meterType, int billingMonth, int billingYear) {
        LocalDate cycleStart = LocalDate.of(billingYear, billingMonth, 1);
        LocalDate cycleEnd = cycleStart.withDayOfMonth(cycleStart.lengthOfMonth());
        return tariffPlanRepository.findApplicableTariff(meterType, cycleStart, cycleEnd)
                .orElseThrow(() -> new BusinessRuleException(
                        "No applicable tariff for " + meterType + " on " + billingMonth + "/" + billingYear));
    }

    public TariffPlan getTariff(Long id) {
        return tariffPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tariff plan not found: " + id));
    }

    private int nextVersion(MeterType meterType) {
        return tariffPlanRepository.findByMeterTypeOrderByVersionNoDesc(meterType).stream()
                .mapToInt(TariffPlan::getVersionNo)
                .max()
                .orElse(0) + 1;
    }

    /** Sets effectiveTo on superseded plans so they never apply retroactively. */
    private void closePreviousVersions(MeterType meterType, LocalDate effectiveFrom) {
        tariffPlanRepository.findByMeterTypeOrderByVersionNoDesc(meterType).stream()
                .filter(t -> t.getEffectiveTo() == null && t.getEffectiveFrom().isBefore(effectiveFrom))
                .forEach(t -> {
                    t.setEffectiveTo(effectiveFrom.minusDays(1));
                    tariffPlanRepository.save(t);
                });
    }

    private void validateTariffRequest(TariffPlanRequest request) {
        if (request.getTariffType() == TariffType.FLAT) {
            if (request.getFlatRatePerUnit() == null) {
                throw new BusinessRuleException("Flat rate per unit is required for FLAT tariff");
            }
        } else if (request.getTiers() == null || request.getTiers().isEmpty()) {
            throw new BusinessRuleException("At least one tier is required for TIERED tariff");
        }
        if (request.getEffectiveTo() != null && request.getEffectiveTo().isBefore(request.getEffectiveFrom())) {
            throw new BusinessRuleException("effectiveTo must be on or after effectiveFrom");
        }
    }

    private void mapTiers(TariffPlan plan, List<TariffTierRequest> tierRequests) {
        if (tierRequests == null) {
            return;
        }
        for (TariffTierRequest tr : tierRequests) {
            TariffTier tier = new TariffTier();
            tier.setTariffPlan(plan);
            tier.setMinUnit(tr.getMinUnit());
            tier.setMaxUnit(tr.getMaxUnit());
            tier.setRatePerUnit(tr.getRatePerUnit());
            plan.getTiers().add(tier);
        }
    }
}
