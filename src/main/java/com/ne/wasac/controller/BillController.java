package com.ne.wasac.controller;

import com.ne.wasac.dto.BillGenerateRequest;
import com.ne.wasac.dto.BillResponse;
import com.ne.wasac.enums.BillStatus;
import com.ne.wasac.enums.SortDirection;
import com.ne.wasac.service.BillService;
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
@RequestMapping("/api/bills")
@RequiredArgsConstructor
@Tag(name = "Bills")
@SecurityRequirement(name = "bearerAuth")
public class BillController {

    private final BillService billService;

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE','OPERATOR')")
    @Operation(summary = "Generate bill", description = "From meter reading. Role: ADMIN, FINANCE, OPERATOR.")
    public BillResponse generate(@Valid @RequestBody BillGenerateRequest request) {
        return billService.generate(request);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('FINANCE')")
    @Operation(summary = "Approve bill", description = "UNPAID to APPROVED. Role: FINANCE.")
    public BillResponse approve(@Parameter(description = "Bill id") @PathVariable Long id) {
        return billService.approve(id);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE','OPERATOR')")
    @Operation(summary = "List or filter bills",
            description = "sortBy: id, billReference, totalAmount, dueDate, billingYear, status. Default sort: billingYear DESC.")
    public List<BillResponse> findAll(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) BillStatus status,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String reference,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false, defaultValue = "DESC") SortDirection sortDir) {
        return billService.filterAndSort(q, status, customerId, reference, sortBy, sortDir);
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE','OPERATOR')")
    @Operation(summary = "Search bills", description = "Filter + sort bills.")
    public List<BillResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) BillStatus status,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String reference,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false, defaultValue = "DESC") SortDirection sortDir) {
        return billService.search(q, status, customerId, reference, sortBy, sortDir);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE','OPERATOR') or (hasRole('CUSTOMER') and @accessGuard.isOwnBill(#id))")
    @Operation(summary = "Get bill by id", description = "Staff or owning customer.")
    public BillResponse findById(@Parameter(description = "Bill id") @PathVariable Long id) {
        return billService.findById(id);
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE','OPERATOR') or (hasRole('CUSTOMER') and @accessGuard.isOwnCustomer(#customerId))")
    @Operation(summary = "List bills by customer", description = "Role: staff or owning customer.")
    public List<BillResponse> findByCustomer(@Parameter(description = "Customer id") @PathVariable Long customerId) {
        return billService.findByCustomer(customerId);
    }
}
