package com.ejada.shared_starter_ratelimit;

public enum RateLimitBypassType {
  SUPER_ADMIN("super_admin"),
  SYSTEM_INTEGRATION("system_integration");

  private final String code;

  RateLimitBypassType(String code) {
    this.code = code;
  }

  public String code() {
    return code;
  }
}
