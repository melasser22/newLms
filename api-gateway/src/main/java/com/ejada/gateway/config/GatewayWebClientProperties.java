package com.ejada.gateway.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.validation.annotation.Validated;

/**
 * Tunable properties for the gateway {@link org.springframework.web.reactive.function.client.WebClient}.
 */
@RefreshScope
@Validated
@ConfigurationProperties(prefix = "gateway.webclient")
public class GatewayWebClientProperties {

  private Duration connectTimeout = Duration.ofSeconds(3);
  private Duration responseTimeout = Duration.ofSeconds(15);
  private Duration readTimeout = Duration.ofSeconds(10);
  private Duration writeTimeout = Duration.ofSeconds(10);
  private boolean wiretap = false;
  private boolean compress = true;
  private int maxInMemorySize = 4 * 1024 * 1024; // 4MB

  public Duration getConnectTimeout() {
    return connectTimeout;
  }

  public void setConnectTimeout(Duration connectTimeout) {
    this.connectTimeout = connectTimeout;
  }

  public Duration getResponseTimeout() {
    return responseTimeout;
  }

  public void setResponseTimeout(Duration responseTimeout) {
    this.responseTimeout = responseTimeout;
  }

  public Duration getReadTimeout() {
    return readTimeout;
  }

  public void setReadTimeout(Duration readTimeout) {
    this.readTimeout = readTimeout;
  }

  public Duration getWriteTimeout() {
    return writeTimeout;
  }

  public void setWriteTimeout(Duration writeTimeout) {
    this.writeTimeout = writeTimeout;
  }

  public boolean isWiretap() {
    return wiretap;
  }

  public void setWiretap(boolean wiretap) {
    this.wiretap = wiretap;
  }

  public boolean isCompress() {
    return compress;
  }

  public void setCompress(boolean compress) {
    this.compress = compress;
  }

  public int getMaxInMemorySize() {
    return maxInMemorySize;
  }

  public void setMaxInMemorySize(int maxInMemorySize) {
    this.maxInMemorySize = maxInMemorySize;
  }
}

