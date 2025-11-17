package com.ejada.sending.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kafka.topics")
public class KafkaTopicsProperties {
  private String emailSend;
  private String emailBulk;
  private String deadLetter;

  public String getEmailSend() {
    return emailSend;
  }

  public void setEmailSend(String emailSend) {
    this.emailSend = emailSend;
  }

  public String getEmailBulk() {
    return emailBulk;
  }

  public void setEmailBulk(String emailBulk) {
    this.emailBulk = emailBulk;
  }

  public String getDeadLetter() {
    return deadLetter;
  }

  public void setDeadLetter(String deadLetter) {
    this.deadLetter = deadLetter;
  }
}
