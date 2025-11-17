package com.ejada.sms.sending.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sms.rate-limit")
public class RateLimitProperties {

  private int capacity = 50;
  private int refillPerMinute = 50;

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
