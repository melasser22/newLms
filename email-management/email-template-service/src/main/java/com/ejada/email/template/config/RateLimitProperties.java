package com.ejada.email.template.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "email.rate-limit")
public record RateLimitProperties(
    long defaultLimitPerMinute,
    long burstMultiplier,
    Duration window,
    Duration idempotencyTtl) {

  public long allowedTokens() {
    return defaultLimitPerMinute * burstMultiplier;
  }
}
