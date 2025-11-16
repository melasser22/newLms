package com.ejada.template.exception;

public class RateLimitExceededException extends RuntimeException {
  public RateLimitExceededException(String tenantId, String action) {
    super("Rate limit exceeded for tenant=" + tenantId + " action=" + action);
  }
}
