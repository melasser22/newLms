package com.ejada.shared_starter_ratelimit;

/**
 * Represents a decision to bypass rate limiting for a privileged request.
 */
public record RateLimitBypassDecision(RateLimitBypassType type, String authority) {

  public String reasonCode() {
    return type.code();
  }
}
