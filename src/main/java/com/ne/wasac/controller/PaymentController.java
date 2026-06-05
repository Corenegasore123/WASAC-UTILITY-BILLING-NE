package com.ne.wasac.controller;

import com.ne.wasac.dto.PaymentRequest;
import com.ne.wasac.dto.PaymentResponse;
import com.ne.wasac.enums.PaymentMethod;
import com.ne.wasac.enums.SortDirection;
import com.ne.wasac.service.PaymentService;
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
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payments")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('FINANCE')")
    @Operation(summary = "Record payment", description = "Partial or full. Role: FINANCE.")
    public PaymentResponse record(@Valid @RequestBody PaymentRequest request) {
        return paymentService.recordPayment(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE','OPERATOR')")
    @Operation(summary = "List or filter payments",
            description = "sortBy: id, amountPaid, paymentDate, paymentMethod, billReference. Default: paymentDate DESC.")
    public List<PaymentResponse> findAll(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long billId,
            @RequestParam(required = false) PaymentMethod method,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false, defaultValue = "DESC") SortDirection sortDir) {
        return paymentService.filterAndSort(q, customerId, billId, method, from, to, sortBy, sortDir);
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE','OPERATOR')")
    @Operation(summary = "Search payments", description = "Filter + sort payments.")
    public List<PaymentResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long billId,
            @RequestParam(required = false) PaymentMethod method,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false, defaultValue = "DESC") SortDirection sortDir) {
        return paymentService.search(q, customerId, billId, method, from, to, sortBy, sortDir);
    }

    @GetMapping("/bill/{billId}")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE','OPERATOR') or (hasRole('CUSTOMER') and @accessGuard.isOwnBill(#billId))")
    @Operation(summary = "List payments by bill", description = "Role: staff or owning customer.")
    public List<PaymentResponse> findByBill(@Parameter(description = "Bill id") @PathVariable Long billId) {
        return paymentService.findByBill(billId);
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE','OPERATOR') or (hasRole('CUSTOMER') and @accessGuard.isOwnCustomer(#customerId))")
    @Operation(summary = "List payments by customer", description = "Role: staff or owning customer.")
    public List<PaymentResponse> findByCustomer(@Parameter(description = "Customer id") @PathVariable Long customerId) {
        return paymentService.findByCustomer(customerId);
    }
}
