package com.ne.wasac.controller;

import com.ne.wasac.dto.MeterRequest;
import com.ne.wasac.dto.MeterResponse;
import com.ne.wasac.enums.MeterStatus;
import com.ne.wasac.enums.MeterType;
import com.ne.wasac.enums.SortDirection;
import com.ne.wasac.service.MeterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/meters")
@RequiredArgsConstructor
@Tag(name = "Meters")
@SecurityRequirement(name = "bearerAuth")
public class MeterController {

    private final MeterService meterService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @Operation(summary = "Assign meter", description = "Meter number WM-##### or EM-#####. Role: ADMIN, OPERATOR.")
    public MeterResponse create(@Valid @RequestBody MeterRequest request) {
        return meterService.create(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','FINANCE')")
    @Operation(summary = "List or filter meters",
            description = "sortBy: id, meterNumber, meterType, status, installationDate, customerName.")
    public List<MeterResponse> findAll(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) MeterStatus status,
            @RequestParam(required = false) MeterType meterType,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false, defaultValue = "ASC") SortDirection sortDir) {
        return meterService.filterAndSort(q, status, meterType, customerId, sortBy, sortDir);
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','FINANCE')")
    @Operation(summary = "Search meters", description = "Filter + sort meters.")
    public List<MeterResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) MeterStatus status,
            @RequestParam(required = false) MeterType meterType,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false, defaultValue = "ASC") SortDirection sortDir) {
        return meterService.search(q, status, meterType, customerId, sortBy, sortDir);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','FINANCE')")
    @Operation(summary = "Get meter by id", description = "Role: ADMIN, OPERATOR, FINANCE.")
    public MeterResponse findById(@Parameter(description = "Meter id") @PathVariable Long id) {
        return meterService.findById(id);
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','FINANCE') or (hasRole('CUSTOMER') and @accessGuard.isOwnCustomer(#customerId))")
    @Operation(summary = "List meters by customer", description = "Role: staff or owning customer.")
    public List<MeterResponse> findByCustomer(@Parameter(description = "Customer id") @PathVariable Long customerId) {
        return meterService.findByCustomer(customerId);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @Operation(summary = "Update meter", description = "Role: ADMIN, OPERATOR.")
    public MeterResponse update(
            @Parameter(description = "Meter id") @PathVariable Long id,
            @Valid @RequestBody MeterRequest request) {
        return meterService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete meter", description = "Role: ADMIN.")
    public void delete(@Parameter(description = "Meter id") @PathVariable Long id) {
        meterService.delete(id);
    }
}
