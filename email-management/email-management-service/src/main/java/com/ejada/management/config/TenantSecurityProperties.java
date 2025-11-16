package com.ejada.management.config;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tenant-security")
public class TenantSecurityProperties {

  private boolean authenticationRequired = true;
  private Map<String, String> tokens = new HashMap<>();
  private RateLimit rateLimit = new RateLimit();

  public boolean isAuthenticationRequired() {
    return authenticationRequired;
  }

  public void setAuthenticationRequired(boolean authenticationRequired) {
    this.authenticationRequired = authenticationRequired;
  }

  public Map<String, String> getTokens() {
    return tokens;
  }

  public void setTokens(Map<String, String> tokens) {
    this.tokens = tokens;
  }

  public RateLimit getRateLimit() {
    return rateLimit;
  }

  public void setRateLimit(RateLimit rateLimit) {
    this.rateLimit = rateLimit;
  }

  public static class RateLimit {
    private boolean enabled = true;
    private int defaultQuota = 120;
    private long windowSeconds = 60;
    private Map<String, Integer> tenantQuotas = new HashMap<>();

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public int getDefaultQuota() {
      return defaultQuota;
    }

    public void setDefaultQuota(int defaultQuota) {
      this.defaultQuota = defaultQuota;
    }

    public long getWindowSeconds() {
      return windowSeconds;
    }

    public void setWindowSeconds(long windowSeconds) {
      this.windowSeconds = windowSeconds;
    }

    public Map<String, Integer> getTenantQuotas() {
      return tenantQuotas;
    }

    public void setTenantQuotas(Map<String, Integer> tenantQuotas) {
      this.tenantQuotas = tenantQuotas;
    }

    public int resolveLimitForTenant(String tenantId) {
      return tenantQuotas.getOrDefault(tenantId, defaultQuota);
    }
  }
}
