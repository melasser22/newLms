package com.ejada.subscription.config;

import com.ejada.subscription.properties.SubscriptionSecurityProperties;
import com.ejada.subscription.security.JwtSigner;
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
    SecretKey key = Keys.hmacShaKeyFor(properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
    final Duration expiry = properties.getJwt().getExpiration();
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
}
