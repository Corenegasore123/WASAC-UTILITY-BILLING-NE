package com.ne.wasac.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

/**
 * Staff with a temporary password may only call /api/auth/change-password.
 * All other endpoints are blocked until the password is changed.
 */
@Component
public class PasswordChangeRequiredFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/auth/login";
    private static final String CHANGE_PASSWORD_PATH = "/api/auth/change-password";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (LOGIN_PATH.equals(request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof SecurityUser user && user.getUser().isMustChangePassword()) {
            if (CHANGE_PASSWORD_PATH.equals(request.getRequestURI())) {
                chain.doFilter(request, response);
                return;
            }
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            OBJECT_MAPPER.writeValue(response.getWriter(), Map.of(
                    "message", "Temporary password must be changed before using other features",
                    "mustChangePassword", true));
            return;
        }
        chain.doFilter(request, response);
    }
}
