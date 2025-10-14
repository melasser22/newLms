package com.ejada.gateway.config;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration backing the gateway aggregation components (REST, SSE and GraphQL).
 * The properties allow overriding downstream service URIs without recompiling the
 * gateway, enabling environment specific routing (local vs. Kubernetes service names).
 */
@RefreshScope
@Validated
@ConfigurationProperties(prefix = "gateway.aggregate")
public class GatewayAggregationProperties {

  private URI tenantServiceUri = URI.create("lb://tenant-service");
  private URI subscriptionServiceUri = URI.create("lb://subscription-service");
  private URI billingServiceUri = URI.create("lb://billing-service");
  private URI analyticsServiceUri = URI.create("lb://analytics-service");
  private URI catalogServiceUri = URI.create("lb://catalog-service");
  private URI auditServiceUri = URI.create("lb://audit-service");
  private Duration requestTimeout = Duration.ofSeconds(8);

  public URI getTenantServiceUri() {
    return tenantServiceUri;
  }

  public void setTenantServiceUri(String tenantServiceUri) {
    if (StringUtils.hasText(tenantServiceUri)) {
      this.tenantServiceUri = URI.create(tenantServiceUri.trim());
    }
  }

  public URI getSubscriptionServiceUri() {
    return subscriptionServiceUri;
  }

  public void setSubscriptionServiceUri(String subscriptionServiceUri) {
    if (StringUtils.hasText(subscriptionServiceUri)) {
      this.subscriptionServiceUri = URI.create(subscriptionServiceUri.trim());
    }
  }

  public URI getBillingServiceUri() {
    return billingServiceUri;
  }

  public void setBillingServiceUri(String billingServiceUri) {
    if (StringUtils.hasText(billingServiceUri)) {
      this.billingServiceUri = URI.create(billingServiceUri.trim());
    }
  }

  public URI getAnalyticsServiceUri() {
    return analyticsServiceUri;
  }

  public void setAnalyticsServiceUri(String analyticsServiceUri) {
    if (StringUtils.hasText(analyticsServiceUri)) {
      this.analyticsServiceUri = URI.create(analyticsServiceUri.trim());
    }
  }

  public URI getCatalogServiceUri() {
    return catalogServiceUri;
  }

  public void setCatalogServiceUri(String catalogServiceUri) {
    if (StringUtils.hasText(catalogServiceUri)) {
      this.catalogServiceUri = URI.create(catalogServiceUri.trim());
    }
  }

  public URI getAuditServiceUri() {
    return auditServiceUri;
  }

  public void setAuditServiceUri(String auditServiceUri) {
    if (StringUtils.hasText(auditServiceUri)) {
      this.auditServiceUri = URI.create(auditServiceUri.trim());
    }
  }

  public Duration getRequestTimeout() {
    return requestTimeout;
  }

  public void setRequestTimeout(Duration requestTimeout) {
    if (requestTimeout != null && !requestTimeout.isNegative() && !requestTimeout.isZero()) {
      this.requestTimeout = requestTimeout;
    }
  }
}
