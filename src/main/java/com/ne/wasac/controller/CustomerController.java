package com.ne.wasac.controller;

import com.ne.wasac.dto.CreateCustomerRequest;
import com.ne.wasac.dto.CreateCustomerResponse;
import com.ne.wasac.dto.CustomerRequest;
import com.ne.wasac.dto.CustomerResponse;
import com.ne.wasac.enums.SortDirection;
import com.ne.wasac.enums.Status;
import com.ne.wasac.service.CustomerService;
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
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Tag(name = "Customers")
@SecurityRequirement(name = "bearerAuth")
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create customer (admin)",
            description = "Admin creates customer profile + login. Temporary password emailed; "
                    + "mustChangePassword=true until first password change. No OTP. Role: ADMIN.")
    public CreateCustomerResponse create(@Valid @RequestBody CreateCustomerRequest request) {
        return customerService.createByAdmin(request);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get my customer account", description = "Returns the logged-in customer's account. Role: CUSTOMER.")
    public CustomerResponse findMe() {
        return customerService.findOwn();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','FINANCE')")
    @Operation(summary = "List or filter customers",
            description = "sortBy: id, fullName, email, phone, nationalId, status. sortDir: ASC/DESC.")
    public List<CustomerResponse> findAll(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false, defaultValue = "ASC") SortDirection sortDir) {
        return customerService.filterAndSort(q, status, email, sortBy, sortDir);
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','FINANCE')")
    @Operation(summary = "Search customers", description = "Filter by q, status, email + sort.")
    public List<CustomerResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false, defaultValue = "ASC") SortDirection sortDir) {
        return customerService.search(q, status, email, sortBy, sortDir);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','FINANCE') or (hasRole('CUSTOMER') and @accessGuard.isOwnCustomer(#id))")
    @Operation(summary = "Get customer by id", description = "Staff or owning customer. Customers should prefer GET /api/customers/me.")
    public CustomerResponse findById(@Parameter(description = "Customer id") @PathVariable Long id) {
        return customerService.findById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @Operation(summary = "Update customer", description = "Role: ADMIN, OPERATOR.")
    public CustomerResponse update(
            @Parameter(description = "Customer id") @PathVariable Long id,
            @Valid @RequestBody CustomerRequest request) {
        return customerService.update(id, request);
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @Operation(summary = "Activate customer", description = "Role: ADMIN, OPERATOR.")
    public CustomerResponse activate(@PathVariable Long id) {
        return customerService.activate(id);
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @Operation(summary = "Deactivate customer",
            description = "Sets customer INACTIVE and deactivates all their meters. Role: ADMIN, OPERATOR.")
    public CustomerResponse deactivate(@PathVariable Long id) {
        return customerService.deactivate(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete customer", description = "Blocked if active meters or unpaid bills. Role: ADMIN.")
    public void delete(@Parameter(description = "Customer id") @PathVariable Long id) {
        customerService.delete(id);
    }
}
