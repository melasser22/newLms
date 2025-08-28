package com.shared.shared_starter_ratelimit;
import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties(prefix="shared.ratelimit")
public class RateLimitProps {
  private int capacity=100; private int refillPerMinute=100; private String keyStrategy="tenant"; // tenant|ip|user
  public int getCapacity(){return capacity;} public void setCapacity(int c){this.capacity=c;}
  public int getRefillPerMinute(){return refillPerMinute;} public void setRefillPerMinute(int r){this.refillPerMinute=r;}
  public String getKeyStrategy(){return keyStrategy;} public void setKeyStrategy(String k){this.keyStrategy=k;}
}
