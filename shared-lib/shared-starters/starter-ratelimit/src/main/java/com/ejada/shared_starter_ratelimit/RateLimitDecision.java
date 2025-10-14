package com.ejada.shared_starter_ratelimit;

import java.time.Duration;

public record RateLimitDecision(
    boolean allowed,
    RateLimitReason reason,
    RateLimitTier tier,
    RateLimitStrategy strategy,
    double remainingTokens,
    Duration retryAfter,
    RateLimitBypassDecision bypassDecision) {

  public boolean isBypass() {
    return bypassDecision != null;
  }

  public static RateLimitDecision bypass(RateLimitBypassDecision bypassDecision, RateLimitTier tier) {
    return new RateLimitDecision(true, RateLimitReason.ALLOWED, tier, null, tier.burstCapacity(), Duration.ZERO,
        bypassDecision);
  }

  public static RateLimitDecision allow(RateLimitTier tier, RateLimitStrategy strategy, double remainingTokens) {
    return new RateLimitDecision(true, RateLimitReason.ALLOWED, tier, strategy, remainingTokens, Duration.ZERO, null);
  }

  public static RateLimitDecision deny(
      RateLimitReason reason, RateLimitTier tier, RateLimitStrategy strategy, double remainingTokens, Duration retryAfter) {
    return new RateLimitDecision(false, reason, tier, strategy, remainingTokens,
        retryAfter == null ? Duration.ZERO : retryAfter, null);
  }
}
