package com.ejada.usage.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
    name = "email_events",
    indexes = {
      @Index(name = "idx_email_events_tenant", columnList = "tenantId"),
      @Index(name = "idx_email_events_occurred", columnList = "occurredAt")
    })
public class EmailEventEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String eventId;

  @Column(nullable = false)
  private String tenantId;

  @Column(nullable = false)
  private String messageId;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private EmailEventType type;

  @Column(nullable = false)
  private Instant occurredAt;

  @Column(nullable = false)
  private Instant receivedAt;

  @Column private String bounceReason;

  protected EmailEventEntity() {}

  public EmailEventEntity(
      Long id,
      String eventId,
      String tenantId,
      String messageId,
      EmailEventType type,
      Instant occurredAt,
      Instant receivedAt,
      String bounceReason) {
    this.id = id;
    this.eventId = eventId;
    this.tenantId = tenantId;
    this.messageId = messageId;
    this.type = type;
    this.occurredAt = occurredAt;
    this.receivedAt = receivedAt;
    this.bounceReason = bounceReason;
  }

  public Long getId() {
    return id;
  }

  public String getEventId() {
    return eventId;
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getMessageId() {
    return messageId;
  }

  public EmailEventType getType() {
    return type;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }

  public Instant getReceivedAt() {
    return receivedAt;
  }

  public String getBounceReason() {
    return bounceReason;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    EmailEventEntity that = (EmailEventEntity) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
