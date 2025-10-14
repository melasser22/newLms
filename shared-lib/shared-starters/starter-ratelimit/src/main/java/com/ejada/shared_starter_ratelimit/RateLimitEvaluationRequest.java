package com.ejada.shared_starter_ratelimit;

import java.util.Optional;
import org.springframework.security.core.Authentication;

/**
 * Context required to evaluate a rate limit decision for an incoming request.
 */
public record RateLimitEvaluationRequest(
    String tenantId,
    String userId,
    String ipAddress,
    String endpoint,
    Authentication authentication) {

  public String safeTenantId() {
    return tenantId == null || tenantId.isBlank() ? "public" : tenantId;
  }

  public String safeUserId() {
    if (userId != null && !userId.isBlank()) {
      return userId;
    }
    return Optional.ofNullable(authentication)
        .map(Authentication::getName)
        .filter(name -> name != null && !name.isBlank())
        .orElse("anonymous");
  }

  public String safeIpAddress() {
    return ipAddress == null || ipAddress.isBlank() ? "unknown" : ipAddress;
  }

  public String safeEndpoint() {
    return endpoint == null || endpoint.isBlank() ? "unknown" : endpoint;
  }
}
