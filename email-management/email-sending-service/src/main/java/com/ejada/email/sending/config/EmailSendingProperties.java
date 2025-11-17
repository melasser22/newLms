package com.ejada.sending.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "email-sending")
public class EmailSendingProperties {

  private int maxAttempts = 3;
  private long backoffInitialMillis = 1000;
  private String templateServiceBaseUrl;
  private String testOverrideEmail;

  public int getMaxAttempts() {
    return maxAttempts;
  }

  public void setMaxAttempts(int maxAttempts) {
    this.maxAttempts = maxAttempts;
  }

  public long getBackoffInitialMillis() {
    return backoffInitialMillis;
  }

  public void setBackoffInitialMillis(long backoffInitialMillis) {
    this.backoffInitialMillis = backoffInitialMillis;
  }

  public String getTemplateServiceBaseUrl() {
    return templateServiceBaseUrl;
  }

  public void setTemplateServiceBaseUrl(String templateServiceBaseUrl) {
    this.templateServiceBaseUrl = templateServiceBaseUrl;
  }

  public String getTestOverrideEmail() {
    return testOverrideEmail;
  }

  public void setTestOverrideEmail(String testOverrideEmail) {
    this.testOverrideEmail = testOverrideEmail;
  }
}
