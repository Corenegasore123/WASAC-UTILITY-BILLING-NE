package com.ne.wasac.dto.auth;

import com.ne.wasac.validation.LettersOnly;
import com.ne.wasac.validation.MinimumAge;
import com.ne.wasac.validation.NationalId;
import com.ne.wasac.validation.PhoneNumber;
import com.ne.wasac.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Customer signup — creates login with ROLE_CUSTOMER, linked by national ID.
 */
@Getter
@Setter
public class SignupRequest {

    @NotBlank
    @Size(min = 2, max = 100)
    @LettersOnly
    private String fullName;

    @NotBlank
    @NationalId
    private String nationalId;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @PhoneNumber
    private String phoneNumber;

    @NotBlank
    @Size(max = 255)
    private String address;

    @Past
    @MinimumAge(18)
    private LocalDate dateOfBirth;

    @NotBlank
    @StrongPassword
    private String password;
}
