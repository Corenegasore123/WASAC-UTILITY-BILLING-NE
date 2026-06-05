package com.ne.wasac.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * Creates and validates HMAC-signed JWT tokens for API authentication.
 */
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long expirationMs;

    /** Loads Base64 secret and token TTL from application properties. */
    public JwtTokenProvider(@Value("${app.jwt.secret}") String secret,
                            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expirationMs = expirationMs;
    }

    /**
     * Builds a signed JWT with subject=email, roles claim, and expiration.
     */
    public String generateToken(Authentication authentication) {
        SecurityUser principal = (SecurityUser) authentication.getPrincipal();
        String roles = principal.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .collect(Collectors.joining(","));

        Date now = new Date();
        return Jwts.builder()
                .subject(principal.getUsername())
                .claim("roles", roles)
                .claim("userId", principal.getUser().getId())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    /** Extracts the email (subject) from a valid token. */
    public String getEmailFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    /** Returns true when signature and expiry are valid. */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
