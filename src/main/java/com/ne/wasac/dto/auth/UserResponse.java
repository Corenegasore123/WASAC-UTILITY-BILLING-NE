package com.ne.wasac.dto.auth;

import com.ne.wasac.enums.RoleName;
import com.ne.wasac.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Set;

@Getter
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String fullName;
    private String nationalId;
    private String email;
    private String phoneNumber;
    private Status status;
    private boolean mustChangePassword;
    private Set<RoleName> roles;
}
