package org.springframework.security.oauth2.jwt;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Minimal stub of Spring Security's {@code Jwt} used purely for unit testing.
 */
public final class Jwt {

    private final String tokenValue;
    private final Instant issuedAt;
    private final Instant expiresAt;
    private final Map<String, Object> headers;
    private final Map<String, Object> claims;

    public Jwt(String tokenValue, Instant issuedAt, Instant expiresAt,
               Map<String, Object> headers, Map<String, Object> claims) {
        this.tokenValue = Objects.requireNonNull(tokenValue, "tokenValue");
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.headers = headers == null ? Map.of() : Map.copyOf(headers);
        this.claims = claims == null ? Map.of() : Map.copyOf(claims);
    }

    public String getTokenValue() {
        return tokenValue;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Map<String, Object> getHeaders() {
        return headers;
    }

    public Map<String, Object> getClaims() {
        return claims;
    }
}
