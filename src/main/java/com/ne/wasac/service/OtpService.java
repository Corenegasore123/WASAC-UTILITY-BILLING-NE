package com.ne.wasac.service;

import com.ne.wasac.enums.OtpPurpose;
import com.ne.wasac.exception.BusinessRuleException;
import com.ne.wasac.model.OtpVerification;
import com.ne.wasac.repository.OtpVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * Generates, emails, and validates one-time passwords for registration.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final OtpVerificationRepository otpRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.otp.length:6}")
    private int otpLength;

    @Value("${app.otp.expiry-minutes:10}")
    private int expiryMinutes;

    /**
     * Creates a new OTP, persists it, and emails it to the user.
     *
     * @return the plain OTP (logged when mail is disabled for local testing)
     */
    @Transactional
    public String generateAndSend(String email, String fullName, OtpPurpose purpose) {
        String otp = generateOtp();
        OtpVerification record = new OtpVerification();
        record.setEmail(email);
        record.setOtpHash(passwordEncoder.encode(otp));
        record.setPurpose(purpose);
        record.setExpiresAt(LocalDateTime.now().plusMinutes(expiryMinutes));
        otpRepository.save(record);

        boolean sent = switch (purpose) {
            case REGISTRATION -> emailService.sendRegistrationOtp(email, fullName, otp, expiryMinutes);
            case PASSWORD_RESET -> emailService.sendPasswordResetOtp(email, fullName, otp, expiryMinutes);
        };
        if (!sent) {
            log.info("OTP for {} purpose {} (mail disabled): {}", email, purpose, otp);
        }
        return otp;
    }

    /** Validates OTP and marks it used. Throws if invalid or expired. */
    @Transactional
    public void verify(String email, String otp, OtpPurpose purpose) {
        OtpVerification record = otpRepository
                .findTopByEmailAndPurposeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                        email, purpose, LocalDateTime.now())
                .orElseThrow(() -> new BusinessRuleException("OTP expired or not found. Request a new code."));

        if (!passwordEncoder.matches(otp, record.getOtpHash())) {
            throw new BusinessRuleException("Invalid OTP code");
        }
        record.setUsed(true);
        otpRepository.save(record);
    }

    private String generateOtp() {
        int bound = (int) Math.pow(10, otpLength);
        int floor = bound / 10;
        int code = floor + RANDOM.nextInt(bound - floor);
        return String.valueOf(code);
    }
}
