package com.ejada.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.validation.annotation.Validated;
import org.springframework.util.StringUtils;

/**
 * Configuration properties backing the gateway BFF (Backend for Frontend)
 * aggregation endpoints. Allows tuning downstream service base URIs without
 * recompiling the gateway which is useful across environments (local, CI,
 * production).
 */
@RefreshScope
@Validated
@ConfigurationProperties(prefix = "gateway.bff")
public class GatewayBffProperties {

  private final TenantDashboardProperties dashboard = new TenantDashboardProperties();

  public TenantDashboardProperties getDashboard() {
    return dashboard;
  }

  public static class TenantDashboardProperties {

    /** Base URI (lb:// or http://) for tenant service lookups. */
    private String tenantServiceUri = "lb://tenant-service";

    /** Base URI for analytics service aggregations. */
    private String analyticsServiceUri = "lb://analytics-service";

    /** Base URI for billing consumption lookups. */
    private String billingServiceUri = "lb://billing-service";

    /** Default analytics period when none supplied by the caller. */
    private String defaultPeriod = "MONTHLY";

    public String getTenantServiceUri() {
      return tenantServiceUri;
    }

    public void setTenantServiceUri(String tenantServiceUri) {
      if (StringUtils.hasText(tenantServiceUri)) {
        this.tenantServiceUri = tenantServiceUri.trim();
      }
    }

    public String getAnalyticsServiceUri() {
      return analyticsServiceUri;
    }

    public void setAnalyticsServiceUri(String analyticsServiceUri) {
      if (StringUtils.hasText(analyticsServiceUri)) {
        this.analyticsServiceUri = analyticsServiceUri.trim();
      }
    }

    public String getBillingServiceUri() {
      return billingServiceUri;
    }

    public void setBillingServiceUri(String billingServiceUri) {
      if (StringUtils.hasText(billingServiceUri)) {
        this.billingServiceUri = billingServiceUri.trim();
      }
    }

    public String getDefaultPeriod() {
      return defaultPeriod;
    }

    public void setDefaultPeriod(String defaultPeriod) {
      if (StringUtils.hasText(defaultPeriod)) {
        this.defaultPeriod = defaultPeriod.trim().toUpperCase();
      }
    }
  }
}
