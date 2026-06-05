package com.ne.wasac.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Returned after registration before OTP verification — no JWT yet.
 */
@Getter
@AllArgsConstructor
public class RegisterPendingResponse {

    private String message;
    private String email;
    private boolean otpSent;
}
