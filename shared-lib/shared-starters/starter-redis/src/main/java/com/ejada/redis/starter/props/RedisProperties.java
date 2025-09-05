package com.ejada.redis.starter.props;

import java.time.Duration;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ConfigurationProperties(prefix = "shared.redis")
public class RedisProperties {
  private String url;                 // redis://user:pass@host:6379/0 or rediss://...
  private String host = "localhost";
  private int port = 6379;
  private String password;
  private int database = 0;
  private boolean ssl = false;
  private String clientName;
  @Builder.Default
  private Duration timeout = Duration.ofSeconds(5);

  private String keyPrefix = "shared";
  @Builder.Default
  private Duration defaultTtl = Duration.ofMinutes(10);
  private Boolean reactive = false;

  private Map<String, CacheSpec> caches;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class CacheSpec {
    private Duration ttl;
    @Builder.Default
    private Boolean cacheNulls = true;         // Spring Data Redis 3.x caches nulls by default
    private String keyPrefixOverride;
  }
}
