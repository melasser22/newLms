package com.shared.crypto;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.crypto.SecretKey;

/**
 * Simple JWT token generator using an HMAC-SHA256 secret.
 */
public class JwtTokenService {

    private final SecretKey key;
    private final Duration defaultTtl;

    private JwtTokenService(SecretKey key, Duration defaultTtl) {
        this.key = key;
        this.defaultTtl = defaultTtl;
    }

    /**
     * Create a service instance using the provided shared secret.
     *
     * @param secret the HMAC secret
     * @return configured service
     */
    public static JwtTokenService withSecret(String secret, Duration defaultTtl) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return new JwtTokenService(key, defaultTtl);
    }

    /**
     * Generate a JWT token with the given subject.
     *
     * @param subject the subject claim value
     * @return signed JWT token
     */
    public String generateToken(String subject) {
        var builder = Jwts.builder()
                .subject(subject)
                .issuedAt(new Date())
                .signWith(key, Jwts.SIG.HS256);
        if (defaultTtl != null) {
            builder.expiration(Date.from(Instant.now().plus(defaultTtl)));
        }
        return builder.compact();
    }

    /**
     * Create a JWT token with additional claims and metadata.
     *
     * @param subject the subject claim value
     * @param tenant tenant identifier to include as a claim
     * @param roles list of role names
     * @param extraClaims additional arbitrary claims
     * @param ttl token time-to-live
     * @return signed JWT token
     */
    public String createToken(String subject, String tenant, List<String> roles,
            Map<String, Object> extraClaims, Duration ttl) {
        var builder = Jwts.builder()
                .subject(subject)
                .issuedAt(new Date())
                .signWith(key, Jwts.SIG.HS256);

        if (tenant != null) {
            builder.claim("tenant", tenant);
        }
        if (roles != null && !roles.isEmpty()) {
            builder.claim("roles", roles);
        }
        if (extraClaims != null && !extraClaims.isEmpty()) {
            extraClaims.forEach(builder::claim);
        }
        Duration effectiveTtl = ttl != null ? ttl : defaultTtl;
        if (effectiveTtl != null) {
            builder.expiration(Date.from(Instant.now().plus(effectiveTtl)));
        }
        return builder.compact();
    }
}
