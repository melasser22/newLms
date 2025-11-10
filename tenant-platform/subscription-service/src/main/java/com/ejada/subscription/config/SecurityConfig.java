package com.ejada.subscription.config;

import com.ejada.subscription.properties.SubscriptionSecurityProperties;
import com.ejada.subscription.security.JwtSigner;
import com.ejada.subscription.security.JwtValidator;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SubscriptionSecurityProperties.class)
public class SecurityConfig {

  @Bean
  public Clock utcClock() {
    return Clock.systemUTC();
  }

  @Bean
  public JwtSigner jwtSigner(final SubscriptionSecurityProperties properties, final Clock clock) {
    SecretKey key = secretKey(properties);
    final Duration expiry = properties.jwt().expiration();
    return subject -> {
      Instant now = clock.instant();
      Instant expiration = now.plus(expiry);
      return Jwts.builder()
          .subject(subject)
          .issuedAt(Date.from(now))
          .expiration(Date.from(expiration))
          .claim("scope", "subscription")
          .signWith(key)
          .compact();
    };
  }

  @Bean
  public JwtValidator jwtValidator(final SubscriptionSecurityProperties properties) {
    SecretKey key = secretKey(properties);
    var parser = Jwts.parser().verifyWith(key).build();
    return token -> {
      if (token == null || token.isBlank()) {
        return false;
      }
      try {
        var claims = parser.parseSignedClaims(token).getPayload();
        return "subscription".equals(claims.get("scope", String.class));
      } catch (JwtException | IllegalArgumentException ex) {
        return false;
      }
    };
  }

  private SecretKey secretKey(final SubscriptionSecurityProperties properties) {
    return Keys.hmacShaKeyFor(properties.jwt().secret().getBytes(StandardCharsets.UTF_8));
  }
}
