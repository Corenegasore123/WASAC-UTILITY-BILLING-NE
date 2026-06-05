package com.ne.wasac.dto;

import com.ne.wasac.enums.RoleName;
import com.ne.wasac.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Set;

@Getter
@AllArgsConstructor
public class ProfileResponse {

    private Long id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private Status status;
    private boolean mustChangePassword;
    private Set<RoleName> roles;
    private Long customerId;
    private String customerName;
}
