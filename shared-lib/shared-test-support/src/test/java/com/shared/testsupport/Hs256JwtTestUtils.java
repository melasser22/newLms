package com.shared.testsupport;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;          
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

public final class Hs256JwtTestUtils {

  private Hs256JwtTestUtils() {}

  public static String generateHs256(String subject, String secret, long ttlSeconds) {
    SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(subject)
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plusSeconds(ttlSeconds)))
        // For JJWT 0.12.x:
        .signWith(key, Jwts.SIG.HS256)
        // For JJWT 0.11.x use:
        // .signWith(key, io.jsonwebtoken.SignatureAlgorithm.HS256)
        .compact();
  }
}
