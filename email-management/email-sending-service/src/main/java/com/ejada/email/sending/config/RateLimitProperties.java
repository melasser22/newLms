package com.ejada.sending.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitProperties {
  private int capacity = 100;
  private int refillPerMinute = 100;

  public int getCapacity() {
    return capacity;
  }

  public void setCapacity(int capacity) {
    this.capacity = capacity;
  }

  public int getRefillPerMinute() {
    return refillPerMinute;
  }

  public void setRefillPerMinute(int refillPerMinute) {
    this.refillPerMinute = refillPerMinute;
  }
}
