package com.ne.wasac.dto.auth;

import com.ne.wasac.validation.LowercaseEmail;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ForgotPasswordRequest {

    @NotBlank
    @LowercaseEmail
    private String email;
}
