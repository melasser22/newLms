package com.ejada.webhook.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sendgrid.webhook")
public class SendGridWebhookProperties {
  private String signingSecret;
  private long toleranceSeconds = 300;

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
}
