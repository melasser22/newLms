package com.ejada.email.management.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ejada.email.management.config.TenantSecurityProperties;
import com.ejada.email.management.config.TenantSecurityProperties.RateLimit;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class TenantRateLimiterTest {

  @Test
  void shouldIgnoreChecksWhenDisabled() {
    TenantSecurityProperties properties = new TenantSecurityProperties();
    RateLimit rateLimit = new RateLimit();
    rateLimit.setEnabled(false);
    properties.setRateLimit(rateLimit);

    TenantRateLimiter rateLimiter = new TenantRateLimiter(properties);

    assertThatCode(() -> rateLimiter.assertWithinQuota("tenant-1", "emails"))
        .doesNotThrowAnyException();
  }

  @Test
  void shouldEnforceQuotaPerTenantAndBucket() {
    TenantSecurityProperties properties = new TenantSecurityProperties();
    RateLimit rateLimit = new RateLimit();
    rateLimit.setEnabled(true);
    rateLimit.setDefaultQuota(2);
    rateLimit.setWindowSeconds(60);
    properties.setRateLimit(rateLimit);

    TenantRateLimiter rateLimiter = new TenantRateLimiter(properties);

    assertThatCode(() -> rateLimiter.assertWithinQuota("tenant-2", "send"))
        .doesNotThrowAnyException();
    assertThatCode(() -> rateLimiter.assertWithinQuota("tenant-2", "send"))
        .doesNotThrowAnyException();

    assertThatThrownBy(() -> rateLimiter.assertWithinQuota("tenant-2", "send"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("tenant-2")
        .hasMessageContaining("send");
  }
}
