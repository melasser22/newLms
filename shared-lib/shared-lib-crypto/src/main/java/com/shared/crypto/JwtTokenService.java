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

    private JwtTokenService(SecretKey key) {
        this.key = key;
    }

    /**
     * Create a service instance using the provided shared secret.
     *
     * @param secret the HMAC secret
     * @return configured service
     */
    public static JwtTokenService withSecret(String secret) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return new JwtTokenService(key);
    }

    /**
     * Generate a JWT token with the given subject.
     *
     * @param subject the subject claim value
     * @return signed JWT token
     */
    public String generateToken(String subject) {
        return Jwts.builder()
                .subject(subject)
                .issuedAt(new Date())
                .signWith(key, Jwts.SIG.HS256)
                .compact();

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
        if (ttl != null) {
            builder.expiration(Date.from(Instant.now().plus(ttl)));
        }
        return builder.compact();
    }
}
