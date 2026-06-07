package com.ne.wasac.dto;

import com.ne.wasac.validation.LettersOnly;
import com.ne.wasac.validation.LowercaseEmail;
import com.ne.wasac.validation.MinimumAge;
import com.ne.wasac.validation.NationalId;
import com.ne.wasac.validation.PhoneNumber;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Admin-only request to create a customer account with a temporary password.
 */
@Getter
@Setter
public class CreateCustomerRequest {

    @NotBlank
    @Size(min = 2, max = 100)
    @LettersOnly
    private String fullName;

    @NotBlank
    @NationalId
    private String nationalId;

    @NotBlank
    @LowercaseEmail
    private String email;

    @NotBlank
    @PhoneNumber
    private String phone;

    @NotBlank
    @Size(max = 255)
    private String address;

    @Past
    @MinimumAge(18)
    private LocalDate dateOfBirth;
}
