package com.shared.redis.starter.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Map;

@ConfigurationProperties(prefix = "shared.redis")
public class RedisProperties {
  private String url;                 // redis://user:pass@host:6379/0 or rediss://...
  private String host = "localhost";
  private int port = 6379;
  private String password;
  private int database = 0;
  private boolean ssl = false;
  private String clientName;
  private Duration timeout = Duration.ofSeconds(5);

  private String keyPrefix = "shared";
  private Duration defaultTtl = Duration.ofMinutes(10);
  private Boolean reactive = false;

  private Map<String, CacheSpec> caches;

  public static class CacheSpec {
    private Duration ttl;
    private Boolean cacheNulls = true;         // Spring Data Redis 3.x caches nulls by default
    private String keyPrefixOverride;
    // getters/setters
    public Duration getTtl() { return ttl; }
    public void setTtl(Duration ttl) { this.ttl = ttl; }
    public Boolean getCacheNulls() { return cacheNulls; }
    public void setCacheNulls(Boolean cacheNulls) { this.cacheNulls = cacheNulls; }
    public String getKeyPrefixOverride() { return keyPrefixOverride; }
    public void setKeyPrefixOverride(String v) { this.keyPrefixOverride = v; }
  }

  // getters/setters for all fields ...
  public String getUrl() { return url; }
  public void setUrl(String url) { this.url = url; }
  public String getHost() { return host; }
  public void setHost(String host) { this.host = host; }
  public int getPort() { return port; }
  public void setPort(int port) { this.port = port; }
  public String getPassword() { return password; }
  public void setPassword(String password) { this.password = password; }
  public int getDatabase() { return database; }
  public void setDatabase(int database) { this.database = database; }
  public boolean isSsl() { return ssl; }
  public void setSsl(boolean ssl) { this.ssl = ssl; }
  public String getClientName() { return clientName; }
  public void setClientName(String clientName) { this.clientName = clientName; }
  public Duration getTimeout() { return timeout; }
  public void setTimeout(Duration timeout) { this.timeout = timeout; }
  public String getKeyPrefix() { return keyPrefix; }
  public void setKeyPrefix(String keyPrefix) { this.keyPrefix = keyPrefix; }
  public Duration getDefaultTtl() { return defaultTtl; }
  public void setDefaultTtl(Duration defaultTtl) { this.defaultTtl = defaultTtl; }
  public Boolean getReactive() { return reactive; }
  public void setReactive(Boolean reactive) { this.reactive = reactive; }
  public Map<String, CacheSpec> getCaches() { return caches; }
  public void setCaches(Map<String, CacheSpec> caches) { this.caches = caches; }
}
