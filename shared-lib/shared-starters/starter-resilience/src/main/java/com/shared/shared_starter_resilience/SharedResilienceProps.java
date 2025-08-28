package com.shared.shared_starter_resilience;
import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties(prefix="shared.resilience")
public class SharedResilienceProps {
  private int httpTimeoutMs=5000; private int connectTimeoutMs=2000;
  public int getHttpTimeoutMs(){return httpTimeoutMs;} public void setHttpTimeoutMs(int v){this.httpTimeoutMs=v;}
  public int getConnectTimeoutMs(){return connectTimeoutMs;} public void setConnectTimeoutMs(int v){this.connectTimeoutMs=v;}
}
