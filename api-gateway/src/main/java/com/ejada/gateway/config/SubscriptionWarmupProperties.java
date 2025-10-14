package com.ejada.gateway.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Configuration for the subscription cache warm-up service.
 */
@ConfigurationProperties(prefix = "gateway.subscription.warmup")
public class SubscriptionWarmupProperties {

  private boolean enabled = true;

  private String tenantServiceUri = "lb://tenant-service/api/v1/tenants";

  private int pageSize = 200;

  private Duration refreshInterval = Duration.ofMinutes(2);

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getTenantServiceUri() {
    return tenantServiceUri;
  }

  public void setTenantServiceUri(String tenantServiceUri) {
    if (StringUtils.hasText(tenantServiceUri)) {
      this.tenantServiceUri = tenantServiceUri.trim();
    }
  }

  public int getPageSize() {
    return pageSize;
  }

  public void setPageSize(int pageSize) {
    this.pageSize = pageSize > 0 ? pageSize : 200;
  }

  public Duration getRefreshInterval() {
    return refreshInterval;
  }

  public void setRefreshInterval(Duration refreshInterval) {
    if (refreshInterval == null || refreshInterval.isZero() || refreshInterval.isNegative()) {
      this.refreshInterval = Duration.ofMinutes(2);
      return;
    }
    this.refreshInterval = refreshInterval;
  }
}

