package com.ejada.shared_starter_ratelimit;

import com.ejada.common.BaseStarterProperties;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Rate limiting properties.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "shared.ratelimit")
public class RateLimitProps implements BaseStarterProperties {

  /** Bucket capacity. */
  private int capacity = 100;

  /** Tokens refilled per minute. */
  private int refillPerMinute = 100;

  /** Strategy for identifying buckets: tenant | ip | user. */
  private String keyStrategy = "tenant";

  /** Duration of the fixed window for rate limiting. */
  private Duration window = Duration.ofMinutes(1);
}
