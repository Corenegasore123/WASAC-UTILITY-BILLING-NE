package com.ne.wasac.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Set;

@Getter
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String email;
    private String fullName;
    private Set<String> roles;
    private boolean mustChangePassword;
}
