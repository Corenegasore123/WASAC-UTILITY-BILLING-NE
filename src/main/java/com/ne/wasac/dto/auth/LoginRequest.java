package com.ne.wasac.dto.auth;

import com.ne.wasac.validation.LowercaseEmail;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Login credentials exchanged for a JWT.
 */
@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @LowercaseEmail
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
}
