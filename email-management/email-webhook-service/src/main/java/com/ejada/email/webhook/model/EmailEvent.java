package com.ejada.email.webhook.model;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class EmailEvent {
  private final UUID id;
  private final String eventId;
  private final EmailEventType type;
  private final String email;
  private final String messageId;
  private final String tenantId;
  private final Instant occurredAt;
  private final Instant processedAt;
  private final Map<String, Object> metadata;
  private final boolean duplicate;

  public EmailEvent(
      UUID id,
      String eventId,
      EmailEventType type,
      String email,
      String messageId,
      String tenantId,
      Instant occurredAt,
      Instant processedAt,
      Map<String, Object> metadata,
      boolean duplicate) {
    this.id = id;
    this.eventId = eventId;
    this.type = type;
    this.email = email;
    this.messageId = messageId;
    this.tenantId = tenantId;
    this.occurredAt = occurredAt;
    this.processedAt = processedAt;
    this.metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    this.duplicate = duplicate;
  }

  public UUID getId() {
    return id;
  }

  public String getEventId() {
    return eventId;
  }

  public EmailEventType getType() {
    return type;
  }

  public String getEmail() {
    return email;
  }

  public String getMessageId() {
    return messageId;
  }

  public String getTenantId() {
    return tenantId;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }

  public Instant getProcessedAt() {
    return processedAt;
  }

  public Map<String, Object> getMetadata() {
    return metadata;
  }

  public boolean isDuplicate() {
    return duplicate;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    EmailEvent emailEvent = (EmailEvent) o;
    return Objects.equals(id, emailEvent.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
