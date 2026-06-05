package com.ne.wasac.controller;

import com.ne.wasac.dto.MeterReadingRequest;
import com.ne.wasac.dto.MeterReadingResponse;
import com.ne.wasac.enums.SortDirection;
import com.ne.wasac.service.MeterReadingService;
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
@RequestMapping("/api/meter-readings")
@RequiredArgsConstructor
@Tag(name = "Meter Readings")
@SecurityRequirement(name = "bearerAuth")
public class MeterReadingController {

    private final MeterReadingService meterReadingService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('OPERATOR')")
    @Operation(summary = "Capture meter reading", description = "Auto-generates bill. Role: OPERATOR.")
    public MeterReadingResponse create(@Valid @RequestBody MeterReadingRequest request) {
        return meterReadingService.create(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','FINANCE')")
    @Operation(summary = "List or filter readings",
            description = "sortBy: id, meterNumber, billingYear, billingMonth, readingDate, currentReading.")
    public List<MeterReadingResponse> findAll(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long meterId,
            @RequestParam(required = false) Integer billingMonth,
            @RequestParam(required = false) Integer billingYear,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false, defaultValue = "DESC") SortDirection sortDir) {
        return meterReadingService.filterAndSort(q, meterId, billingMonth, billingYear, sortBy, sortDir);
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','FINANCE')")
    @Operation(summary = "Search meter readings", description = "Filter + sort readings.")
    public List<MeterReadingResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long meterId,
            @RequestParam(required = false) Integer billingMonth,
            @RequestParam(required = false) Integer billingYear,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false, defaultValue = "DESC") SortDirection sortDir) {
        return meterReadingService.search(q, meterId, billingMonth, billingYear, sortBy, sortDir);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','FINANCE')")
    @Operation(summary = "Get reading by id", description = "Role: ADMIN, OPERATOR, FINANCE.")
    public MeterReadingResponse findById(@Parameter(description = "Reading id") @PathVariable Long id) {
        return meterReadingService.findById(id);
    }

    @GetMapping("/meter/{meterId}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','FINANCE')")
    @Operation(summary = "List readings by meter", description = "Role: ADMIN, OPERATOR, FINANCE.")
    public List<MeterReadingResponse> findByMeter(@Parameter(description = "Meter id") @PathVariable Long meterId) {
        return meterReadingService.findByMeter(meterId);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @Operation(summary = "Update reading", description = "Role: ADMIN, OPERATOR.")
    public MeterReadingResponse update(
            @Parameter(description = "Reading id") @PathVariable Long id,
            @Valid @RequestBody MeterReadingRequest request) {
        return meterReadingService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete reading", description = "Role: ADMIN.")
    public void delete(@Parameter(description = "Reading id") @PathVariable Long id) {
        meterReadingService.delete(id);
    }
}
