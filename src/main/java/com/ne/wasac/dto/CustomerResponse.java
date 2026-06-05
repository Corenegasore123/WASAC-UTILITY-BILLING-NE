package com.ne.wasac.dto;

import com.ne.wasac.enums.Status;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

/** Customer profile returned by the API. */
@Getter
@Builder
public class CustomerResponse {
    private Long id;
    private String fullName;
    private String nationalId;
    private String email;
    private String phone;
    private String address;
    private LocalDate dateOfBirth;
    private Status status;
}
