package com.ejada.management.service;

import com.ejada.management.config.TenantSecurityProperties;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TenantRateLimiter {

  private final TenantSecurityProperties properties;
  private final Map<String, RequestWindow> windows = new ConcurrentHashMap<>();

  public TenantRateLimiter(TenantSecurityProperties properties) {
    this.properties = properties;
  }

  public void assertWithinQuota(String tenantId, String bucket) {
    TenantSecurityProperties.RateLimit rateLimit = properties.getRateLimit();
    if (!rateLimit.isEnabled()) {
      return;
    }
    String key = tenantId + ":" + bucket;
    RequestWindow window = windows.compute(key, (k, existing) -> resetIfNeeded(existing, rateLimit));
    int current = window.counter.incrementAndGet();
    int limit = rateLimit.resolveLimitForTenant(tenantId);
    if (limit > 0 && current > limit) {
      throw new ResponseStatusException(
          HttpStatus.TOO_MANY_REQUESTS,
          "Rate limit exceeded for tenant " + tenantId + " bucket " + bucket);
    }
  }

  private RequestWindow resetIfNeeded(
      RequestWindow existing, TenantSecurityProperties.RateLimit rateLimit) {
    Instant now = Instant.now();
    if (existing == null) {
      return new RequestWindow(now, new AtomicInteger(0));
    }
    long windowSeconds = Math.max(rateLimit.getWindowSeconds(), 1);
    if (now.isAfter(existing.windowStart.plusSeconds(windowSeconds))) {
      return new RequestWindow(now, new AtomicInteger(0));
    }
    return existing;
  }

  private static final class RequestWindow {
    private final Instant windowStart;
    private final AtomicInteger counter;

    private RequestWindow(Instant windowStart, AtomicInteger counter) {
      this.windowStart = windowStart;
      this.counter = counter;
    }
  }
}
