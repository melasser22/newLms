package com.ejada.crypto;

import com.ejada.common.constants.JwtClaims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.crypto.SecretKey;

/**
 * Simple JWT token generator using an HMAC-SHA256 secret.
 */
public class JwtTokenService {

    private final SecretKey key;
    private final Duration defaultTtl;

    public JwtTokenService(SecretKey key, Duration defaultTtl) {
        this.key = Objects.requireNonNull(key, "key");
        this.defaultTtl = defaultTtl;
    }

    /**
     * Create a service instance using the provided shared secret.
     *
     * @param secret the HMAC secret (Base64-encoded)
     * @return configured service
     */
    public static JwtTokenService withSecret(String secret, Duration defaultTtl) {
        return new JwtTokenService(createKey(secret), defaultTtl);
    }

    /**
     * Build an HMAC key from the configured Base64 secret.
     */
    public static SecretKey createKey(String secret) {
        byte[] keyBytes = CryptoUtils.safeBase64Decode(secret, "JWT signing secret");
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("decoded secret must be at least 32 bytes");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generate a JWT token with the given subject.
     *
     * @param subject the subject claim value
     * @return signed JWT token
     */
    public String generateToken(String subject) {
        return startBuilder(subject, defaultTtl).compact();
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
        JwtBuilder builder = startBuilder(subject, ttl);
        putClaimIfPresent(builder, JwtClaims.TENANT, tenant);
        if (roles != null && !roles.isEmpty()) {
            builder.claim(JwtClaims.ROLES, roles);
        }
        if (extraClaims != null && !extraClaims.isEmpty()) {
            extraClaims.forEach(builder::claim);
        }
        return builder.compact();
    }

    private JwtBuilder startBuilder(String subject, Duration ttlOverride) {
        Duration effectiveTtl = ttlOverride != null ? ttlOverride : defaultTtl;
        JwtBuilder builder = Jwts.builder()
                .subject(Objects.requireNonNull(subject, "subject"))
                .issuedAt(new Date())
                .signWith(key, Jwts.SIG.HS256);
        if (effectiveTtl != null) {
            builder.expiration(Date.from(Instant.now().plus(effectiveTtl)));
        }
        return builder;
    }

    private static void putClaimIfPresent(JwtBuilder builder, String name, Object value) {
        if (value != null) {
            builder.claim(name, value);
        }
    }
}
