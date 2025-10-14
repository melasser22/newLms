package com.ejada.shared_starter_ratelimit;

import java.time.Duration;

/**
 * Immutable snapshot of an effective rate limit tier.
 */
public record RateLimitTier(String name, int requestsPerMinute, int burstCapacity, Duration window) {

  public RateLimitTier {
    if (name == null || name.isBlank()) {
      name = "BASIC";
    }
    if (requestsPerMinute < 0) {
      requestsPerMinute = 0;
    }
    if (burstCapacity < 0) {
      burstCapacity = 0;
    }
    if (window == null || window.isNegative() || window.isZero()) {
      window = Duration.ofMinutes(1);
    }
  }

  public double tokensPerMillisecond() {
    if (requestsPerMinute <= 0) {
      return 0D;
    }
    return (double) requestsPerMinute / (double) window.toMillis();
  }
}
