package com.ne.wasac.dto.auth;

import com.ne.wasac.enums.RoleName;
import com.ne.wasac.validation.LettersOnly;
import com.ne.wasac.validation.LowercaseEmail;
import com.ne.wasac.validation.NationalId;
import com.ne.wasac.validation.PhoneNumber;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Admin-only request to create Operator or Finance staff with temporary password.
 */
@Getter
@Setter
public class CreateStaffRequest {

    @NotBlank
    @Size(min = 2, max = 100)
    @LettersOnly
    private String fullName;

    @NotBlank
    @LowercaseEmail
    private String email;

    @NotBlank
    @PhoneNumber
    private String phoneNumber;

    @NotBlank
    @NationalId
    private String nationalId;

    @NotNull(message = "Role must be OPERATOR or FINANCE")
    private RoleName role;
}
