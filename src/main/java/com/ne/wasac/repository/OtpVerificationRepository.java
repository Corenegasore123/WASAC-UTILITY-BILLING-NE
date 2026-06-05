package com.ne.wasac.repository;

import com.ne.wasac.enums.OtpPurpose;
import com.ne.wasac.model.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    Optional<OtpVerification> findTopByEmailAndPurposeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
            String email, OtpPurpose purpose, LocalDateTime now);

    @Modifying
    long deleteByExpiresAtBefore(LocalDateTime cutoff);
}
