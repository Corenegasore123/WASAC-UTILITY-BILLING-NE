package com.ne.wasac.service;

import com.ne.wasac.dto.MeterRequest;
import com.ne.wasac.dto.MeterResponse;
import com.ne.wasac.enums.AuditAction;
import com.ne.wasac.enums.MeterStatus;
import com.ne.wasac.enums.MeterType;
import com.ne.wasac.enums.SortDirection;
import com.ne.wasac.enums.Status;
import com.ne.wasac.util.QuerySort;
import com.ne.wasac.exception.BusinessRuleException;
import com.ne.wasac.exception.ResourceNotFoundException;
import com.ne.wasac.model.Customer;
import com.ne.wasac.model.Meter;
import com.ne.wasac.repository.MeterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Assigns and maintains utility meters for customers.
 */
@Service
@RequiredArgsConstructor
public class MeterService {

    private static final Map<String, Comparator<MeterResponse>> SORT_FIELDS = Map.of(
            "id", Comparator.comparing(MeterResponse::getId),
            "meterNumber", Comparator.comparing(MeterResponse::getMeterNumber, String.CASE_INSENSITIVE_ORDER),
            "meterType", Comparator.comparing(m -> m.getMeterType().name()),
            "status", Comparator.comparing(m -> m.getStatus().name()),
            "installationDate", Comparator.comparing(MeterResponse::getInstallationDate),
            "customerName", Comparator.comparing(MeterResponse::getCustomerName, String.CASE_INSENSITIVE_ORDER));

    private final MeterRepository meterRepository;
    private final CustomerService customerService;
    private final AuditService auditService;

    /**
     * Registers a meter on an ACTIVE customer. Enforces WM/EM prefix vs meter type.
     */
    @Transactional
    public MeterResponse create(MeterRequest request) {
        validateMeterNumberFormat(request);
        validateUniqueMeterNumber(null, request.getMeterNumber());
        Customer customer = customerService.getCustomer(request.getCustomerId());
        if (customer.getStatus() != Status.ACTIVE) {
            throw new BusinessRuleException("Cannot assign meter to inactive customer");
        }
        Meter meter = mapToEntity(new Meter(), request, customer);
        return DtoMapper.toMeterResponse(meterRepository.save(meter));
    }

    @Transactional(readOnly = true)
    public List<MeterResponse> findAll(String sortBy, SortDirection sortDir) {
        return sort(meterRepository.findAll().stream().map(DtoMapper::toMeterResponse).toList(), sortBy, sortDir);
    }

    @Transactional(readOnly = true)
    public MeterResponse findById(Long id) {
        return DtoMapper.toMeterResponse(getMeter(id));
    }

    @Transactional(readOnly = true)
    public List<MeterResponse> findByCustomer(Long customerId) {
        return meterRepository.findByCustomerId(customerId).stream().map(DtoMapper::toMeterResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<MeterResponse> search(String q, MeterStatus status, MeterType meterType, Long customerId,
                                      String sortBy, SortDirection sortDir) {
        return sort(meterRepository.search(blankToNull(q), status, meterType, customerId)
                .stream().map(DtoMapper::toMeterResponse).toList(), sortBy, sortDir);
    }

    @Transactional(readOnly = true)
    public List<MeterResponse> filterAndSort(String q, MeterStatus status, MeterType meterType, Long customerId,
                                             String sortBy, SortDirection sortDir) {
        if (blankToNull(q) != null || status != null || meterType != null || customerId != null) {
            return search(q, status, meterType, customerId, sortBy, sortDir);
        }
        return findAll(sortBy, sortDir);
    }

    private List<MeterResponse> sort(List<MeterResponse> items, String sortBy, SortDirection sortDir) {
        return QuerySort.apply(items, sortBy, sortDir == null ? SortDirection.ASC : sortDir,
                SORT_FIELDS, "meterNumber");
    }

    @Transactional
    public MeterResponse update(Long id, MeterRequest request) {
        Meter meter = getMeter(id);
        validateMeterNumberFormat(request);
        validateUniqueMeterNumber(id, request.getMeterNumber());
        Customer customer = customerService.getCustomer(request.getCustomerId());
        if (customer.getStatus() != Status.ACTIVE) {
            throw new BusinessRuleException("Cannot assign meter to inactive customer");
        }
        MeterStatus oldStatus = meter.getStatus();
        mapToEntity(meter, request, customer);
        Meter saved = meterRepository.save(meter);
        if (oldStatus != saved.getStatus()) {
            auditService.log(AuditAction.METER_STATUS_CHANGED, "Meter", saved.getId(),
                    oldStatus.name(), saved.getStatus().name());
        }
        return DtoMapper.toMeterResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        if (!meterRepository.existsById(id)) {
            throw new ResourceNotFoundException("Meter not found: " + id);
        }
        meterRepository.deleteById(id);
    }

    public Meter getMeter(Long id) {
        return meterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meter not found: " + id));
    }

    /** WM prefix required for water; EM for electricity. */
    private void validateMeterNumberFormat(MeterRequest request) {
        String prefix = request.getMeterType() == MeterType.WATER ? "WM-" : "EM-";
        if (!request.getMeterNumber().startsWith(prefix)) {
            throw new BusinessRuleException("Meter number prefix must match meter type (" + prefix + "XXXXX)");
        }
    }

    private void validateUniqueMeterNumber(Long id, String meterNumber) {
        meterRepository.findByMeterNumber(meterNumber).ifPresent(existing -> {
            if (id == null || !existing.getId().equals(id)) {
                throw new BusinessRuleException("Meter number already exists");
            }
        });
    }

    private Meter mapToEntity(Meter meter, MeterRequest request, Customer customer) {
        meter.setMeterNumber(request.getMeterNumber());
        meter.setMeterType(request.getMeterType());
        meter.setInstallationDate(request.getInstallationDate());
        meter.setStatus(request.getStatus());
        meter.setCustomer(customer);
        return meter;
    }

    private String blankToNull(String value) {
        return QuerySort.blankToNull(value);
    }
}
