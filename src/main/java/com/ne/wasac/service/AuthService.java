package com.ne.wasac.service;

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
import com.ne.wasac.enums.OtpPurpose;
import com.ne.wasac.enums.RoleName;
import com.ne.wasac.enums.Status;
import com.ne.wasac.exception.BusinessRuleException;
import com.ne.wasac.exception.ResourceNotFoundException;
import com.ne.wasac.model.AppUser;
import com.ne.wasac.model.Customer;
import com.ne.wasac.model.Role;
import com.ne.wasac.repository.AppUserRepository;
import com.ne.wasac.repository.CustomerRepository;
import com.ne.wasac.repository.RoleRepository;
import com.ne.wasac.security.JwtTokenProvider;
import com.ne.wasac.security.SecurityUser;
import com.ne.wasac.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final OtpService otpService;

    /**
     * Customer signup: creates customer profile and login. Always assigns ROLE_CUSTOMER.
     * Account stays INACTIVE until OTP is verified.
     */
    @Transactional
    public RegisterPendingResponse signup(SignupRequest request) {
        if (appUserRepository.existsByEmail(request.getEmail())) {
            throw new BusinessRuleException("Email already registered");
        }
        if (appUserRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new BusinessRuleException("Phone number already registered");
        }
        if (appUserRepository.existsByNationalId(request.getNationalId())
                || customerRepository.existsByNationalId(request.getNationalId())) {
            throw new BusinessRuleException("National ID already exists");
        }

        Customer customer = createCustomerFromSignup(request);

        Role customerRole = roleRepository.findByName(RoleName.ROLE_CUSTOMER)
                .orElseThrow(() -> new ResourceNotFoundException("Customer role not found"));

        AppUser user = new AppUser();
        user.setFullName(request.getFullName());
        user.setNationalId(request.getNationalId());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(Status.INACTIVE);
        user.setMustChangePassword(false);
        user.setPendingEmailVerification(true);
        user.getRoles().add(customerRole);
        user.setCustomer(customer);
        appUserRepository.save(user);

        otpService.generateAndSend(user.getEmail(), user.getFullName(), OtpPurpose.REGISTRATION);
        return new RegisterPendingResponse(
                "Account created. Verify the OTP sent to your email to activate your account.",
                user.getEmail(),
                true);
    }

    private Customer createCustomerFromSignup(SignupRequest request) {
        Customer customer = new Customer();
        customer.setFullName(request.getFullName());
        customer.setNationalId(request.getNationalId());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhoneNumber());
        customer.setAddress(request.getAddress());
        customer.setDateOfBirth(request.getDateOfBirth());
        customer.setStatus(Status.INACTIVE);
        return customerRepository.save(customer);
    }

    /**
     * Step 2: Verify OTP and activate user (+ linked customer profile).
     */
    @Transactional
    public AuthResponse verifyOtp(VerifyOtpRequest request) {
        AppUser user = appUserRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("No registration found for this email"));

        if (user.getStatus() == Status.ACTIVE) {
            throw new BusinessRuleException("Account is already activated. Please login.");
        }
        if (!user.isPendingEmailVerification()) {
            throw new BusinessRuleException("Account deactivated. Contact an administrator to reactivate.");
        }

        otpService.verify(request.getEmail(), request.getOtp(), OtpPurpose.REGISTRATION);

        user.setStatus(Status.ACTIVE);
        user.setPendingEmailVerification(false);
        if (user.getCustomer() != null && user.getCustomer().getStatus() == Status.INACTIVE) {
            user.getCustomer().setStatus(Status.ACTIVE);
        }
        appUserRepository.save(user);

        return buildAuthResponseForUser(user);
    }

    /**
     * Resend OTP for a pending (INACTIVE) registration.
     */
    @Transactional
    public RegisterPendingResponse resendOtp(ResendOtpRequest request) {
        AppUser user = appUserRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("No registration found for this email"));

        if (user.getStatus() == Status.ACTIVE) {
            throw new BusinessRuleException("Account is already activated. Please login.");
        }
        if (!user.isPendingEmailVerification()) {
            throw new BusinessRuleException("Account deactivated. Contact an administrator to reactivate.");
        }

        otpService.generateAndSend(user.getEmail(), user.getFullName(), OtpPurpose.REGISTRATION);
        return new RegisterPendingResponse(
                "A new OTP has been sent to your email.",
                user.getEmail(),
                true);
    }

    /**
     * Sends a password recovery OTP to an active account.
     */
    @Transactional
    public RegisterPendingResponse forgotPassword(ForgotPasswordRequest request) {
        AppUser user = appUserRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("No account found for this email"));

        if (user.getStatus() == Status.INACTIVE) {
            if (user.isPendingEmailVerification()) {
                throw new BusinessRuleException(
                        "Account not activated. Verify your registration OTP before recovering your password.");
            }
            throw new BusinessRuleException("Account deactivated. Contact an administrator to reactivate.");
        }

        otpService.generateAndSend(user.getEmail(), user.getFullName(), OtpPurpose.PASSWORD_RESET);
        return new RegisterPendingResponse(
                "Password recovery OTP sent to your email.",
                user.getEmail(),
                true);
    }

    /**
     * Verifies recovery OTP and sets a new password. Clears mustChangePassword.
     */
    @Transactional
    public ChangePasswordResponse resetPassword(ResetPasswordRequest request) {
        AppUser user = appUserRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("No account found for this email"));

        if (user.getStatus() == Status.INACTIVE) {
            if (user.isPendingEmailVerification()) {
                throw new BusinessRuleException("Account not activated. Complete registration OTP verification first.");
            }
            throw new BusinessRuleException("Account deactivated. Contact an administrator to reactivate.");
        }

        otpService.verify(request.getEmail(), request.getOtp(), OtpPurpose.PASSWORD_RESET);

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BusinessRuleException("New password must differ from the current password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        appUserRepository.save(user);

        return new ChangePasswordResponse("Password reset successfully");
    }

    public AuthResponse login(LoginRequest request) {
        AppUser user = appUserRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessRuleException("Invalid email or password"));

        if (user.getStatus() == Status.INACTIVE) {
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                throw new BusinessRuleException("Invalid email or password");
            }
            if (user.isPendingEmailVerification()) {
                throw new BusinessRuleException(
                        "Account not activated. Verify your email with the OTP sent during registration.");
            }
            throw new BusinessRuleException("Account deactivated.");
        }

        return buildAuthResponse(request.getEmail(), request.getPassword());
    }

    @Transactional
    public ChangePasswordResponse changePassword(ChangePasswordRequest request) {
        SecurityUser current = SecurityUtils.currentUser();
        AppUser user = appUserRepository.findByEmail(current.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BusinessRuleException("Current password is incorrect");
        }
        if (request.getCurrentPassword().equals(request.getNewPassword())) {
            throw new BusinessRuleException("New password must differ from the temporary password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        appUserRepository.save(user);

        SecurityUser refreshed = new SecurityUser(user);
        Authentication updated = new UsernamePasswordAuthenticationToken(
                refreshed, null, refreshed.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(updated);
        return new ChangePasswordResponse("Password changed successfully");
    }

    private AuthResponse buildAuthResponse(String email, String rawPassword) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, rawPassword));
        SecurityUser principal = (SecurityUser) authentication.getPrincipal();
        return toAuthResponse(principal, jwtTokenProvider.generateToken(authentication));
    }

    private AuthResponse buildAuthResponseForUser(AppUser user) {
        SecurityUser principal = new SecurityUser(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return toAuthResponse(principal, jwtTokenProvider.generateToken(authentication));
    }

    private AuthResponse toAuthResponse(SecurityUser principal, String token) {
        Set<String> roles = principal.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .collect(Collectors.toSet());
        return new AuthResponse(token, principal.getUsername(), principal.getUser().getFullName(), roles,
                principal.getUser().isMustChangePassword());
    }
}
