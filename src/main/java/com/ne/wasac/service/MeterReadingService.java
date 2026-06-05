package com.ne.wasac.service;

import com.ne.wasac.dto.BillGenerateRequest;
import com.ne.wasac.dto.MeterReadingRequest;
import com.ne.wasac.dto.MeterReadingResponse;
import com.ne.wasac.enums.AuditAction;
import com.ne.wasac.enums.MeterStatus;
import com.ne.wasac.enums.SortDirection;
import com.ne.wasac.util.QuerySort;
import com.ne.wasac.exception.BusinessRuleException;
import com.ne.wasac.exception.ResourceNotFoundException;
import com.ne.wasac.model.Meter;
import com.ne.wasac.model.MeterReading;
import com.ne.wasac.repository.MeterReadingRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Captures monthly meter readings and auto-generates bills.
 */
@Service
public class MeterReadingService {

    private static final Map<String, Comparator<MeterReadingResponse>> SORT_FIELDS = Map.of(
            "id", Comparator.comparing(MeterReadingResponse::getId),
            "meterNumber", Comparator.comparing(MeterReadingResponse::getMeterNumber, String.CASE_INSENSITIVE_ORDER),
            "billingYear", Comparator.comparing(MeterReadingResponse::getBillingYear),
            "billingMonth", Comparator.comparing(MeterReadingResponse::getBillingMonth),
            "readingDate", Comparator.comparing(MeterReadingResponse::getReadingDate),
            "currentReading", Comparator.comparing(MeterReadingResponse::getCurrentReading));

    private final MeterReadingRepository meterReadingRepository;
    private final MeterService meterService;
    private final BillService billService;
    private final AuditService auditService;

    public MeterReadingService(MeterReadingRepository meterReadingRepository,
                               MeterService meterService,
                               @Lazy BillService billService,
                               AuditService auditService) {
        this.meterReadingRepository = meterReadingRepository;
        this.meterService = meterService;
        this.billService = billService;
        this.auditService = auditService;
    }

    /**
     * Saves a reading and triggers bill generation. Enforces one reading per meter/month/year.
     */
    @Transactional
    public MeterReadingResponse create(MeterReadingRequest request) {
        Meter meter = meterService.getMeter(request.getMeterId());
        validateReadingRules(meter, request, null);
        MeterReading reading = mapToEntity(new MeterReading(), request, meter);
        MeterReading saved = meterReadingRepository.save(reading);
        auditService.log(AuditAction.METER_READING_CAPTURED, "MeterReading", saved.getId(), null,
                saved.getCurrentReading().toPlainString());
        autoGenerateBill(saved);
        return DtoMapper.toMeterReadingResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<MeterReadingResponse> findAll(String sortBy, SortDirection sortDir) {
        return sort(meterReadingRepository.findAll().stream().map(DtoMapper::toMeterReadingResponse).toList(),
                sortBy, sortDir);
    }

    @Transactional(readOnly = true)
    public List<MeterReadingResponse> search(String q, Long meterId, Integer billingMonth, Integer billingYear,
                                             String sortBy, SortDirection sortDir) {
        return sort(meterReadingRepository.search(QuerySort.blankToNull(q), meterId, billingMonth, billingYear)
                .stream().map(DtoMapper::toMeterReadingResponse).toList(), sortBy, sortDir);
    }

    @Transactional(readOnly = true)
    public List<MeterReadingResponse> filterAndSort(String q, Long meterId, Integer billingMonth, Integer billingYear,
                                                    String sortBy, SortDirection sortDir) {
        if (QuerySort.blankToNull(q) != null || meterId != null || billingMonth != null || billingYear != null) {
            return search(q, meterId, billingMonth, billingYear, sortBy, sortDir);
        }
        return findAll(sortBy, sortDir);
    }

    private List<MeterReadingResponse> sort(List<MeterReadingResponse> items, String sortBy, SortDirection sortDir) {
        return QuerySort.apply(items, sortBy, sortDir == null ? SortDirection.DESC : sortDir,
                SORT_FIELDS, "billingYear");
    }

    @Transactional(readOnly = true)
    public MeterReadingResponse findById(Long id) {
        return DtoMapper.toMeterReadingResponse(getReading(id));
    }

    @Transactional(readOnly = true)
    public List<MeterReadingResponse> findByMeter(Long meterId) {
        return meterReadingRepository.findByMeterIdOrderByBillingYearDescBillingMonthDesc(meterId)
                .stream().map(DtoMapper::toMeterReadingResponse).toList();
    }

    @Transactional
    public MeterReadingResponse update(Long id, MeterReadingRequest request) {
        MeterReading reading = getReading(id);
        Meter meter = meterService.getMeter(request.getMeterId());
        validateReadingRules(meter, request, id);
        mapToEntity(reading, request, meter);
        return DtoMapper.toMeterReadingResponse(meterReadingRepository.save(reading));
    }

    @Transactional
    public void delete(Long id) {
        if (!meterReadingRepository.existsById(id)) {
            throw new ResourceNotFoundException("Meter reading not found: " + id);
        }
        meterReadingRepository.deleteById(id);
    }

    public MeterReading getReading(Long id) {
        return meterReadingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meter reading not found: " + id));
    }

    /** Validates active meter, reading order, uniqueness, and continuity with last reading. */
    private void validateReadingRules(Meter meter, MeterReadingRequest request, Long excludeId) {
        if (request.getReadingDate() != null && request.getReadingDate().isAfter(LocalDate.now())) {
            throw new BusinessRuleException("Reading date cannot be in the future");
        }
        if (meter.getStatus() != MeterStatus.ACTIVE) {
            throw new BusinessRuleException("Meter must be active to record readings");
        }
        if (request.getCurrentReading().compareTo(request.getPreviousReading()) <= 0) {
            throw new BusinessRuleException("Current reading must be greater than previous reading");
        }
        meterReadingRepository.findByMeterIdAndBillingMonthAndBillingYear(
                request.getMeterId(), request.getBillingMonth(), request.getBillingYear()
        ).ifPresent(existing -> {
            if (excludeId == null || !existing.getId().equals(excludeId)) {
                throw new BusinessRuleException("Reading already exists for this meter and billing period");
            }
        });
        Optional<MeterReading> last = meterReadingRepository
                .findTopByMeterIdOrderByBillingYearDescBillingMonthDesc(request.getMeterId());
        last.ifPresent(previous -> {
            if (excludeId == null || !previous.getId().equals(excludeId)) {
                if (request.getPreviousReading().compareTo(previous.getCurrentReading()) != 0) {
                    throw new BusinessRuleException(
                            "Previous reading must match the last recorded current reading for this meter");
                }
            }
        });
    }

    private MeterReading mapToEntity(MeterReading reading, MeterReadingRequest request, Meter meter) {
        reading.setMeter(meter);
        reading.setPreviousReading(request.getPreviousReading());
        reading.setCurrentReading(request.getCurrentReading());
        reading.setReadingDate(request.getReadingDate());
        reading.setBillingMonth(request.getBillingMonth());
        reading.setBillingYear(request.getBillingYear());
        return reading;
    }

    /** Auto-generates bill after reading unless one already exists. */
    private void autoGenerateBill(MeterReading reading) {
        BillGenerateRequest billRequest = new BillGenerateRequest();
        billRequest.setMeterReadingId(reading.getId());
        billRequest.setBillingMonth(reading.getBillingMonth());
        billRequest.setBillingYear(reading.getBillingYear());
        billRequest.setApplyPenalty(false);
        try {
            billService.generate(billRequest);
        } catch (BusinessRuleException ex) {
            if (!ex.getMessage().contains("Bill already exists")) {
                throw ex;
            }
        }
    }
}
