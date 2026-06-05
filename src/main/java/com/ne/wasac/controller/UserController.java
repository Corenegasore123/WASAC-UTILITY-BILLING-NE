package com.ne.wasac.controller;

import com.ne.wasac.dto.auth.CreateStaffRequest;
import com.ne.wasac.dto.auth.UserResponse;
import com.ne.wasac.enums.RoleName;
import com.ne.wasac.enums.SortDirection;
import com.ne.wasac.enums.Status;
import com.ne.wasac.service.UserService;
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
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create Operator or Finance staff",
            description = "Admin creates staff (fullName, nationalId, email, phone, role). "
                    + "Temporary password emailed; must change on first login. Role: ADMIN.")
    public UserResponse createStaff(@Valid @RequestBody CreateStaffRequest request) {
        return userService.createStaff(request);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List or filter users",
            description = "Status: ACTIVE or INACTIVE only. sortBy: id, fullName, nationalId, email, phoneNumber, status.")
    public List<UserResponse> findAll(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) RoleName role,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false, defaultValue = "ASC") SortDirection sortDir) {
        return userService.filterAndSort(q, status, role, email, sortBy, sortDir);
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Search users", description = "Same as GET /api/users with filters. Role: ADMIN.")
    public List<UserResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) RoleName role,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false, defaultValue = "ASC") SortDirection sortDir) {
        return userService.search(q, status, role, email, sortBy, sortDir);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user by id", description = "Role: ADMIN.")
    public UserResponse findById(@Parameter(description = "User id") @PathVariable Long id) {
        return userService.findById(id);
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Upgrade or revoke user role",
            description = "Assigns OPERATOR, FINANCE, or CUSTOMER. "
                    + "Email sent only when upgraded to Operator or Finance. Role: ADMIN.")
    public UserResponse updateRole(
            @Parameter(description = "User id") @PathVariable Long id,
            @Parameter(description = "New role") @RequestParam RoleName role) {
        return userService.updateRole(id, role);
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate user",
            description = "Sets status to INACTIVE (record kept for audit). Deactivated users cannot log in.")
    public UserResponse deactivate(@Parameter(description = "User id") @PathVariable Long id) {
        return userService.deactivate(id);
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activate user", description = "Sets status to ACTIVE. Role: ADMIN.")
    public UserResponse activate(@Parameter(description = "User id") @PathVariable Long id) {
        return userService.activate(id);
    }
}
