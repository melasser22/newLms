package com.ejada.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties controlling custom tracing behaviour within the gateway.
 */
@ConfigurationProperties(prefix = "gateway.tracing")
public class GatewayTracingProperties {

  private EnhancedTags enhancedTags = new EnhancedTags();

  public EnhancedTags getEnhancedTags() {
    return enhancedTags;
  }

  public void setEnhancedTags(EnhancedTags enhancedTags) {
    this.enhancedTags = enhancedTags == null ? new EnhancedTags() : enhancedTags;
  }

  public static class EnhancedTags {

    private boolean enabled;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }
  }
}
