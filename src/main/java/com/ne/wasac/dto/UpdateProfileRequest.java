package com.ne.wasac.dto;

import com.ne.wasac.validation.LettersOnly;
import com.ne.wasac.validation.PhoneNumber;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileRequest {

    @NotBlank
    @Size(min = 2, max = 100)
    @LettersOnly
    private String fullName;

    @NotBlank
    @PhoneNumber
    private String phoneNumber;
}
