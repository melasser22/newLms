package com.lms.tenant.events.core;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_event")
public class OutboxEvent {
  @Id @Column(name = "event_id", nullable = false) private UUID eventId;
  @Column(name = "event_type", nullable = false) private String eventType;
  @Column(name = "aggregate_type", nullable = false) private String aggregateType;
  @Column(name = "aggregate_id", nullable = false) private UUID aggregateId;
  @Column(name = "occurred_at", nullable = false) private Instant occurredAt;
  @Column(name = "payload", columnDefinition = "jsonb", nullable = false) private String payloadJson;
  @Column(name = "headers", columnDefinition = "jsonb", nullable = false) private String headersJson;
  @Enumerated(EnumType.STRING) @Column(name = "status", nullable = false) private OutboxStatus status = OutboxStatus.NEW;
  @Column(name = "attempts", nullable = false) private int attempts = 0;
  @Column(name = "published_at") private Instant publishedAt;

  @PrePersist public void prePersist(){ if(eventId==null) eventId=UUID.randomUUID(); if(occurredAt==null) occurredAt=Instant.now(); }

  // getters/setters
  public UUID getEventId(){return eventId;} public void setEventId(UUID v){eventId=v;}
  public String getEventType(){return eventType;} public void setEventType(String v){eventType=v;}
  public String getAggregateType(){return aggregateType;} public void setAggregateType(String v){aggregateType=v;}
  public UUID getAggregateId(){return aggregateId;} public void setAggregateId(UUID v){aggregateId=v;}
  public Instant getOccurredAt(){return occurredAt;} public void setOccurredAt(Instant v){occurredAt=v;}
  public String getPayloadJson(){return payloadJson;} public void setPayloadJson(String v){payloadJson=v;}
  public String getHeadersJson(){return headersJson;} public void setHeadersJson(String v){headersJson=v;}
  public OutboxStatus getStatus(){return status;} public void setStatus(OutboxStatus v){status=v;}
  public int getAttempts(){return attempts;} public void setAttempts(int v){attempts=v;}
  public Instant getPublishedAt(){return publishedAt;} public void setPublishedAt(Instant v){publishedAt=v;}
}
