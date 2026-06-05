package com.ne.wasac.controller;

import com.ne.wasac.dto.ProfileResponse;
import com.ne.wasac.dto.UpdateProfileRequest;
import com.ne.wasac.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@Tag(name = "Profile")
@SecurityRequirement(name = "bearerAuth")
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    @Operation(summary = "Get my profile",
            description = "Current authenticated user. Customers use this to view their login account and linked customer id.")
    public ProfileResponse getProfile() {
        return profileService.getProfile();
    }

    @PutMapping
    @Operation(summary = "Update my profile",
            description = "Update full name and phone. Customers use this to update their own account details.")
    public ProfileResponse updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return profileService.updateProfile(request);
    }
}
