package com.ejada.email.webhook;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sendgrid.webhook")
public class SendgridWebhookProperties {

  private String signingSecret;
  private long toleranceSeconds = 300;
  private List<String> allowedIps = List.of();
  private String kafkaTopic = "sendgrid.email-events";
  private long deduplicationTtlSeconds = 86400;

  public String getSigningSecret() {
    return signingSecret;
  }

  public void setSigningSecret(String signingSecret) {
    this.signingSecret = signingSecret;
  }

  public long getToleranceSeconds() {
    return toleranceSeconds;
  }

  public void setToleranceSeconds(long toleranceSeconds) {
    this.toleranceSeconds = toleranceSeconds;
  }

  public List<String> getAllowedIps() {
    return List.copyOf(allowedIps);
  }

  public void setAllowedIps(List<String> allowedIps) {
    this.allowedIps = allowedIps == null ? List.of() : List.copyOf(allowedIps);
  }

  public String getKafkaTopic() {
    return kafkaTopic;
  }

  public void setKafkaTopic(String kafkaTopic) {
    this.kafkaTopic = kafkaTopic;
  }

  public long getDeduplicationTtlSeconds() {
    return deduplicationTtlSeconds;
  }

  public void setDeduplicationTtlSeconds(long deduplicationTtlSeconds) {
    this.deduplicationTtlSeconds = deduplicationTtlSeconds;
  }
}
