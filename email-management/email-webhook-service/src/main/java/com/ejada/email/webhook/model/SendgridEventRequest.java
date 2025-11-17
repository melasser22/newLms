package com.ejada.email.webhook.model;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SendgridEventRequest {

  private String event;

  private String email;

  @JsonProperty("timestamp")
  private long timestamp;

  @JsonProperty("sg_event_id")
  private String eventId;

  @JsonProperty("sg_message_id")
  private String messageId;

  @JsonProperty("smtp-id")
  private String smtpId;

  private String reason;
  private String url;
  private String ip;

  @JsonProperty("useragent")
  private String userAgent;

  @JsonProperty("category")
  private String category;

  @JsonProperty("custom_args")
  private Map<String, Object> customArgs = new HashMap<>();

  private final Map<String, Object> additionalFields = new HashMap<>();

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

  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  public String getEventId() {
    return eventId;
  }

  public void setEventId(String eventId) {
    this.eventId = eventId;
  }

  public String getMessageId() {
    return messageId;
  }

  public void setMessageId(String messageId) {
    this.messageId = messageId;
  }

  public String getSmtpId() {
    return smtpId;
  }

  public void setSmtpId(String smtpId) {
    this.smtpId = smtpId;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getIp() {
    return ip;
  }

  public void setIp(String ip) {
    this.ip = ip;
  }

  public String getUserAgent() {
    return userAgent;
  }

  public void setUserAgent(String userAgent) {
    this.userAgent = userAgent;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public Map<String, Object> getCustomArgs() {
    return Map.copyOf(customArgs);
  }

  public void setCustomArgs(Map<String, Object> customArgs) {
    this.customArgs = customArgs == null ? Map.of() : new HashMap<>(customArgs);
  }

  public Instant occurredAt() {
    return Instant.ofEpochSecond(timestamp);
  }

  public Map<String, Object> getAdditionalFields() {
    return Map.copyOf(additionalFields);
  }

  @JsonAnySetter
  public void putAdditionalField(String key, Object value) {
    if (!"event".equals(key)
        && !"email".equals(key)
        && !"timestamp".equals(key)
        && !"sg_event_id".equals(key)
        && !"sg_message_id".equals(key)
        && !"smtp-id".equals(key)
        && !"reason".equals(key)
        && !"url".equals(key)
        && !"ip".equals(key)
        && !"useragent".equals(key)
        && !"category".equals(key)
        && !"custom_args".equals(key)) {
      additionalFields.put(key, value);
    }
  }

  @Override
  public String toString() {
    return "SendgridEventRequest{"
        + "event='"
        + event
        + '\''
        + ", email='"
        + email
        + '\''
        + ", timestamp="
        + timestamp
        + ", eventId='"
        + eventId
        + '\''
        + ", messageId='"
        + messageId
        + '\''
        + '}';
  }

  @Override
  public int hashCode() {
    return Objects.hash(event, email, timestamp, eventId, messageId);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    SendgridEventRequest other = (SendgridEventRequest) obj;
    return timestamp == other.timestamp
        && Objects.equals(event, other.event)
        && Objects.equals(email, other.email)
        && Objects.equals(eventId, other.eventId)
        && Objects.equals(messageId, other.messageId);
  }
}
