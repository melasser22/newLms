package com.ejada.gateway.config;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/** Configuration for the GraphQL proxy endpoint exposed by the gateway. */
@ConfigurationProperties(prefix = "gateway.graphql")
public class GatewayGraphqlProperties {

  private boolean enabled = false;

  private URI upstreamUri;

  private Duration timeout = Duration.ofSeconds(10);

  private int maxDepth = 10;

  private int maxBreadth = 50;

  private int maxComplexity = 200;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public URI getUpstreamUri() {
    return upstreamUri;
  }

  public void setUpstreamUri(URI upstreamUri) {
    this.upstreamUri = upstreamUri;
  }

  public void setUpstreamUri(String upstreamUri) {
    this.upstreamUri = StringUtils.hasText(upstreamUri) ? URI.create(upstreamUri.trim()) : null;
  }

  public Duration getTimeout() {
    return timeout;
  }

  public void setTimeout(Duration timeout) {
    if (timeout == null || timeout.isNegative() || timeout.isZero()) {
      return;
    }
    this.timeout = timeout;
  }

  public int getMaxDepth() {
    return maxDepth;
  }

  public void setMaxDepth(int maxDepth) {
    if (maxDepth > 0) {
      this.maxDepth = maxDepth;
    }
  }

  public int getMaxBreadth() {
    return maxBreadth;
  }

  public void setMaxBreadth(int maxBreadth) {
    if (maxBreadth > 0) {
      this.maxBreadth = maxBreadth;
    }
  }

  public int getMaxComplexity() {
    return maxComplexity;
  }

  public void setMaxComplexity(int maxComplexity) {
    if (maxComplexity > 0) {
      this.maxComplexity = maxComplexity;
    }
  }
}

