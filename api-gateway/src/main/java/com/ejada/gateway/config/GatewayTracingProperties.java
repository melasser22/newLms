package com.ejada.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Gateway specific tracing configuration.
 */
@ConfigurationProperties(prefix = "gateway.tracing")
public class GatewayTracingProperties {

  private final EnhancedTags enhancedTags = new EnhancedTags();

  public EnhancedTags getEnhancedTags() {
    return enhancedTags;
  }

  public boolean isEnhancedTagsEnabled() {
    return enhancedTags.isEnabled();
  }

  public static class EnhancedTags {

    private boolean enabled = false;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }
  }
}
