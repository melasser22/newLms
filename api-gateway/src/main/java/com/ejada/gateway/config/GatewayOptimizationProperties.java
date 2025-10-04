package com.ejada.gateway.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Operational tuning toggles for optimisation and cost management.
 */
@Validated
@ConfigurationProperties(prefix = "gateway.optimization")
public class GatewayOptimizationProperties {

  private boolean enabled = true;
  private double samplingProbability = 0.01d;
  private List<String> warmupTenants = new ArrayList<>();
  private Duration warmupTimeout = Duration.ofSeconds(10);
  private final Pool pool = new Pool();

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public double getSamplingProbability() {
    return samplingProbability;
  }

  public void setSamplingProbability(double samplingProbability) {
    this.samplingProbability = samplingProbability;
  }

  public List<String> getWarmupTenants() {
    return warmupTenants;
  }

  public void setWarmupTenants(List<String> warmupTenants) {
    this.warmupTenants = (warmupTenants != null) ? new ArrayList<>(warmupTenants) : new ArrayList<>();
  }

  public Duration getWarmupTimeout() {
    return warmupTimeout;
  }

  public void setWarmupTimeout(Duration warmupTimeout) {
    this.warmupTimeout = warmupTimeout;
  }

  public Pool getPool() {
    return pool;
  }

  public static class Pool {

    private int maxConnections = 200;
    private int pendingAcquireMaxCount = 500;
    private Duration maxIdleTime = Duration.ofSeconds(30);
    private Duration maxLifeTime = Duration.ofMinutes(10);
    private Duration evictInBackground = Duration.ofSeconds(30);
    private Duration acquireTimeout = Duration.ofSeconds(5);
    private boolean metricsEnabled = true;

    public int getMaxConnections() {
      return maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
      this.maxConnections = maxConnections;
    }

    public int getPendingAcquireMaxCount() {
      return pendingAcquireMaxCount;
    }

    public void setPendingAcquireMaxCount(int pendingAcquireMaxCount) {
      this.pendingAcquireMaxCount = pendingAcquireMaxCount;
    }

    public Duration getMaxIdleTime() {
      return maxIdleTime;
    }

    public void setMaxIdleTime(Duration maxIdleTime) {
      this.maxIdleTime = maxIdleTime;
    }

    public Duration getMaxLifeTime() {
      return maxLifeTime;
    }

    public void setMaxLifeTime(Duration maxLifeTime) {
      this.maxLifeTime = maxLifeTime;
    }

    public Duration getEvictInBackground() {
      return evictInBackground;
    }

    public void setEvictInBackground(Duration evictInBackground) {
      this.evictInBackground = evictInBackground;
    }

    public Duration getAcquireTimeout() {
      return acquireTimeout;
    }

    public void setAcquireTimeout(Duration acquireTimeout) {
      this.acquireTimeout = acquireTimeout;
    }

    public boolean isMetricsEnabled() {
      return metricsEnabled;
    }

    public void setMetricsEnabled(boolean metricsEnabled) {
      this.metricsEnabled = metricsEnabled;
    }
  }
}
