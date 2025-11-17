package com.ejada.sms.sending.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sms.kafka.topics")
public class KafkaTopicsProperties {

  private String smsSend = "sms.send.request";
  private String smsBulk = "sms.send.bulk";

  public String getSmsSend() {
    return smsSend;
  }

  public void setSmsSend(String smsSend) {
    this.smsSend = smsSend;
  }

  public String getSmsBulk() {
    return smsBulk;
  }

  public void setSmsBulk(String smsBulk) {
    this.smsBulk = smsBulk;
  }
}
