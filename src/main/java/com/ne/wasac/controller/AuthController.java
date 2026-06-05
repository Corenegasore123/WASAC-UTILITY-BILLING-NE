package com.ne.wasac.controller;

import com.ne.wasac.dto.auth.AuthResponse;
import com.ne.wasac.dto.auth.ChangePasswordRequest;
import com.ne.wasac.dto.auth.ChangePasswordResponse;
import com.ne.wasac.dto.auth.ForgotPasswordRequest;
import com.ne.wasac.dto.auth.LoginRequest;
import com.ne.wasac.dto.auth.ResetPasswordRequest;
import com.ne.wasac.dto.auth.RegisterPendingResponse;
import com.ne.wasac.dto.auth.ResendOtpRequest;
import com.ne.wasac.dto.auth.SignupRequest;
import com.ne.wasac.dto.auth.VerifyOtpRequest;
import com.ne.wasac.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Self-signup (OTP) or admin-created customer (temp password) → Login")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirements
    @Operation(summary = "Step 1 — Customer self-signup",
            description = "Public. Customer sets own password (mustChangePassword=false). "
                    + "Sends OTP — no JWT until verify-otp. Admin can also create customers via POST /api/customers.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "OTP sent — account pending activation",
                    content = @Content(schema = @Schema(implementation = RegisterPendingResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error or duplicate email / phone / national ID")
    })
    public RegisterPendingResponse signup(@Valid @RequestBody SignupRequest request) {
        return authService.signup(request);
    }

    @PostMapping("/verify-otp")
    @SecurityRequirements
    @Operation(summary = "Step 2 — Verify signup OTP and activate",
            description = "Activates self-signup account only. Admin-deactivated accounts need admin activate. Public.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account activated",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid or expired OTP")
    })
    public AuthResponse verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return authService.verifyOtp(request);
    }

    @PostMapping("/resend-otp")
    @SecurityRequirements
    @Operation(summary = "Resend signup OTP",
            description = "Sends OTP only for self-signup awaiting verification. "
                    + "Not for admin-deactivated accounts. Public.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "New OTP sent",
                    content = @Content(schema = @Schema(implementation = RegisterPendingResponse.class))),
            @ApiResponse(responseCode = "400", description = "Account already active or not found")
    })
    public RegisterPendingResponse resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        return authService.resendOtp(request);
    }

    @PostMapping("/forgot-password")
    @SecurityRequirements
    @Operation(summary = "Forgot password — request OTP",
            description = "Public. Sends a recovery OTP to an active account's email.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recovery OTP sent",
                    content = @Content(schema = @Schema(implementation = RegisterPendingResponse.class))),
            @ApiResponse(responseCode = "400", description = "Account not activated"),
            @ApiResponse(responseCode = "404", description = "Email not found")
    })
    public RegisterPendingResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return authService.forgotPassword(request);
    }

    @PostMapping("/reset-password")
    @SecurityRequirements
    @Operation(summary = "Reset password with OTP",
            description = "Public. Verifies recovery OTP and sets a new password. Clears mustChangePassword.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password reset",
                    content = @Content(schema = @Schema(implementation = ChangePasswordResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid or expired OTP, or weak password")
    })
    public ChangePasswordResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return authService.resetPassword(request);
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Login",
            description = "Returns JWT. Account must be activated (OTP verified). Public.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "JWT issued",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid credentials, not activated, or account deactivated"),
            @ApiResponse(responseCode = "401", description = "Invalid email or password")
    })
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/change-password")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Change password",
            description = "Required after first login with temporary staff password. Role: any authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password changed",
                    content = @Content(schema = @Schema(implementation = ChangePasswordResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid current password or weak new password")
    })
    public ChangePasswordResponse changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        return authService.changePassword(request);
    }

}
