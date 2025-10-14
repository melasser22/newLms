package com.ejada.shared_starter_ratelimit;

/**
 * Reasons returned by the rate limiter when a request is rejected.
 */
public enum RateLimitReason {
  ALLOWED("allowed"),
  RATE_LIMIT_HIT("rate_limit_hit"),
  BURST_CAPACITY_FULL("burst_capacity_full"),
  QUOTA_EXCEEDED("quota_exceeded");

  private final String code;

  RateLimitReason(String code) {
    this.code = code;
  }

  public String code() {
    return code;
  }
}
