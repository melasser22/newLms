package com.ejada.webhook.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class SendGridEvent {
  private String event;
  private String email;
  private String sgMessageId;
  private Instant timestamp;
  private final Map<String, Object> customArgs = new HashMap<>();

  public String getEvent() {
    return event;
  }

  public void setEvent(String event) {
    this.event = event;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getSgMessageId() {
    return sgMessageId;
  }

  public void setSgMessageId(String sgMessageId) {
    this.sgMessageId = sgMessageId;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Instant timestamp) {
    this.timestamp = timestamp;
  }

  public Map<String, Object> getCustomArgs() {
    return customArgs;
  }

  @JsonAnySetter
  public void addCustomArg(String key, Object value) {
    customArgs.put(key, value);
  }
}
