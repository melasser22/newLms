package com.ejada.audit.starter.persistence.entity;

import com.ejada.common.constants.HeaderNames;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
public class AuditEventEntity {
  @Id
  private UUID id;
  @Column(name="ts_utc") private Instant tsUtc;
  @Column(name = HeaderNames.X_TENANT_ID) private String xTenantId;
  @Column(name="actor_id") private String actorId;
  @Column(name="actor_username") private String actorUsername;
  @Column(name="action") private String action;
  @Column(name="entity_type") private String entityType;
  @Column(name="entity_id") private String entityId;
  @Column(name="outcome") private String outcome;
  @Column(name="message") private String message;
  @Lob @Column(name="payload") private String payload;
  // getters/setters omitted for brevity
}
