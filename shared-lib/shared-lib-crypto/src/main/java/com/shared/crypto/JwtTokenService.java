package com.shared.crypto;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Simple service for creating HS256 signed JWT tokens.
 */
public class JwtTokenService {

    private final SecretKey secretKey;

    public JwtTokenService(SecretKey secretKey) {
        this.secretKey = Objects.requireNonNull(secretKey, "secretKey");
    }

    /**
     * Create a JWT with the given subject, tenant and roles.
     *
     * @param subject the subject (sub) claim
     * @param tenant  tenant identifier
     * @param roles   roles assigned to the subject
     * @param claims  additional claims, may be null
     * @param ttl     token time-to-live
     * @return compact JWT string
     */
    public String createToken(String subject, String tenant, List<String> roles, Map<String, Object> claims, Duration ttl) {
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(tenant, "tenant");
        Objects.requireNonNull(roles, "roles");
        Objects.requireNonNull(ttl, "ttl");
        Instant now = Instant.now();
        JwtBuilder builder = Jwts.builder()
                .subject(subject)
                .claim("tenant", tenant)
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)));
        if (claims != null && !claims.isEmpty()) {
            claims.forEach(builder::claim);
        }
        return builder
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Convenience factory creating service from a raw UTF-8 secret.
     */
    public static JwtTokenService withSecret(String secret) {
        Objects.requireNonNull(secret, "secret");
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return new JwtTokenService(key);
    }
}
