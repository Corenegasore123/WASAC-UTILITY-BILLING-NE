package com.ne.wasac.dto.auth;

import com.ne.wasac.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePasswordRequest {
    @NotBlank
    private String currentPassword;

    @NotBlank
    @StrongPassword
    private String newPassword;
}
