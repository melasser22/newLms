package com.ejada.shared_starter_resilience;

import com.ejada.common.BaseStarterProperties;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties for Resilience4j integration.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "shared.resilience")
public class SharedResilienceProps implements BaseStarterProperties {

  /** HTTP timeout in milliseconds. */
  private int httpTimeoutMs = 5000;

  /** Connection timeout in milliseconds. */
  private int connectTimeoutMs = 2000;
}
