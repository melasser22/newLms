package com.shared.crypto;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
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
}
