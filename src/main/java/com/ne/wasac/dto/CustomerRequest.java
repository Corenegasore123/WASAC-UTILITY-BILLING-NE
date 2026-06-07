package com.ne.wasac.dto;

import com.ne.wasac.enums.Status;
import com.ne.wasac.validation.LettersOnly;
import com.ne.wasac.validation.LowercaseEmail;
import com.ne.wasac.validation.MinimumAge;
import com.ne.wasac.validation.NationalId;
import com.ne.wasac.validation.PhoneNumber;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Request body for creating or updating a customer profile.
 */
@Getter
@Setter
public class CustomerRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    @LettersOnly // Rejects digits in customer names
    private String fullName;

    @NotBlank(message = "National ID is required")
    @NationalId // Exactly 16 digits; uniqueness checked in service
    private String nationalId;

    @NotBlank(message = "Email is required")
    @LowercaseEmail
    private String email;

    @NotBlank(message = "Phone is required")
    @PhoneNumber
    private String phone;

    @NotBlank(message = "Address is required")
    @Size(max = 255, message = "Address must not exceed 255 characters")
    private String address;

    @Past(message = "Date of birth must be in the past")
    @MinimumAge(18) // Customer must be at least 18 when DOB is provided
    private LocalDate dateOfBirth;

    @NotNull(message = "Status is required")
    private Status status;
}
