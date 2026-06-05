package com.ne.wasac.controller;

import com.ne.wasac.dto.TariffPlanRequest;
import com.ne.wasac.dto.TariffPlanResponse;
import com.ne.wasac.enums.MeterType;
import com.ne.wasac.enums.SortDirection;
import com.ne.wasac.enums.TariffType;
import com.ne.wasac.service.TariffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/tariffs")
@RequiredArgsConstructor
@Tag(name = "Tariffs")
@SecurityRequirement(name = "bearerAuth")
public class TariffController {

    private final TariffService tariffService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create tariff", description = "Versioned FLAT or TIERED tariff. Role: ADMIN.")
    public TariffPlanResponse create(@Valid @RequestBody TariffPlanRequest request) {
        return tariffService.create(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE','OPERATOR')")
    @Operation(summary = "List or filter tariffs",
            description = "sortBy: id, versionNo, meterType, tariffType, effectiveFrom. Default: versionNo DESC.")
    public List<TariffPlanResponse> findAll(
            @RequestParam(required = false) MeterType meterType,
            @RequestParam(required = false) TariffType tariffType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveTo,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false, defaultValue = "DESC") SortDirection sortDir) {
        return tariffService.filterAndSort(meterType, tariffType, effectiveFrom, effectiveTo, sortBy, sortDir);
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE','OPERATOR')")
    @Operation(summary = "Search tariffs", description = "Filter by meterType, tariffType, effective date range + sort.")
    public List<TariffPlanResponse> search(
            @RequestParam(required = false) MeterType meterType,
            @RequestParam(required = false) TariffType tariffType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveTo,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false, defaultValue = "DESC") SortDirection sortDir) {
        return tariffService.search(meterType, tariffType, effectiveFrom, effectiveTo, sortBy, sortDir);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE','OPERATOR')")
    @Operation(summary = "Get tariff by id", description = "Role: ADMIN, FINANCE, OPERATOR.")
    public TariffPlanResponse findById(@Parameter(description = "Tariff id") @PathVariable Long id) {
        return tariffService.findById(id);
    }

    @GetMapping("/meter-type/{meterType}")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE','OPERATOR')")
    @Operation(summary = "List tariffs by meter type", description = "WATER or ELECTRICITY.")
    public List<TariffPlanResponse> findByMeterType(
            @Parameter(description = "WATER or ELECTRICITY") @PathVariable MeterType meterType) {
        return tariffService.findByMeterType(meterType);
    }
}
