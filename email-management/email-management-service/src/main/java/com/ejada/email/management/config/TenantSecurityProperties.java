package com.ejada.management.config;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
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
    return Map.copyOf(tokens);
  }

  public void setTokens(Map<String, String> tokens) {
    this.tokens = new HashMap<>(tokens);
  }

  public RateLimit getRateLimit() {
    return new RateLimit(rateLimit);
  }

  public void setRateLimit(RateLimit rateLimit) {
    this.rateLimit = new RateLimit(rateLimit);
  }

  public static class RateLimit {
    private boolean enabled = true;
    private int defaultQuota = 120;
    private long windowSeconds = 60;
    private Map<String, Integer> tenantQuotas = new HashMap<>();

    public RateLimit() {
      // default constructor
    }

    private RateLimit(RateLimit other) {
      this.enabled = other.enabled;
      this.defaultQuota = other.defaultQuota;
      this.windowSeconds = other.windowSeconds;
      this.tenantQuotas = new HashMap<>(other.tenantQuotas);
    }

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
      return Map.copyOf(tenantQuotas);
    }

    public void setTenantQuotas(Map<String, Integer> tenantQuotas) {
      this.tenantQuotas = tenantQuotas.entrySet().stream()
          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b, HashMap::new));
    }

    public int resolveLimitForTenant(String tenantId) {
      return tenantQuotas.getOrDefault(tenantId, defaultQuota);
    }
  }
}
