package com.shared.audit.starter.api;

import com.shared.audit.starter.api.context.Actor;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class AuditEvent {
  private final String schemaVersion;
  private final UUID eventId;
  private final Instant timestamp;
  private final String tenantId;
  private final Actor actor;
  private final AuditAction action;
  private final String entityType;
  private final String entityId;
  private final AuditOutcome outcome;
  private final Sensitivity sensitivity;
  private final DataClass dataClass;
  private final Map<String, Object> resource;
  private final Map<String, Object> diff;
  private final Map<String, Object> metadata;
  private final String message;
  private final Map<String, Object> payload;

  private AuditEvent(Builder b) {
    this.schemaVersion = b.schemaVersion;
    this.eventId = b.eventId==null?UUID.randomUUID():b.eventId;
    this.timestamp = b.timestamp==null?Instant.now():b.timestamp;
    this.tenantId = b.tenantId;
    this.actor = b.actor;
    this.action = b.action;
    this.entityType = b.entityType;
    this.entityId = b.entityId;
    this.outcome = b.outcome;
    this.sensitivity = b.sensitivity;
    this.dataClass = b.dataClass;
    this.resource = Map.copyOf(b.resource);
    this.diff = Map.copyOf(b.diff);
    this.metadata = Map.copyOf(b.metadata);
    this.message = b.message;
    this.payload = Map.copyOf(b.payload);
  }

  public String getSchemaVersion() { return schemaVersion; }
  public UUID getEventId() { return eventId; }
  public Instant getTimestamp() { return timestamp; }
  public String getTenantId() { return tenantId; }
  public Actor getActor() { return actor; }
  public AuditAction getAction() { return action; }
  public String getEntityType() { return entityType; }
  public String getEntityId() { return entityId; }
  public AuditOutcome getOutcome() { return outcome; }
  public Sensitivity getSensitivity() { return sensitivity; }
  public DataClass getDataClass() { return dataClass; }
  public Map<String,Object> getResource() { return resource; }
  public Map<String,Object> getDiff() { return diff; }
  public Map<String,Object> getMetadata() { return metadata; }
  public String getMessage() { return message; }
  public Map<String,Object> getPayload() { return payload; }

  public static Builder builder() { return new Builder(); }

  public static final class Builder {
    private String schemaVersion = "1.0";
    private UUID eventId;
    private java.time.Instant timestamp;
    private String tenantId;
    private Actor actor;
    private AuditAction action = AuditAction.OTHER;
    private String entityType;
    private String entityId;
    private AuditOutcome outcome = AuditOutcome.SUCCESS;
    private Sensitivity sensitivity = Sensitivity.INTERNAL;
    private DataClass dataClass = DataClass.NONE;
    private Map<String,Object> resource = new HashMap<>();
    private Map<String,Object> diff = new HashMap<>();
    private Map<String,Object> metadata = new HashMap<>();
    private String message;
    private Map<String,Object> payload = new HashMap<>();

    public Builder schemaVersion(String v) { this.schemaVersion = v; return this; }
    public Builder eventId(UUID v) { this.eventId = v; return this; }
    public Builder timestamp(java.time.Instant v) { this.timestamp = v; return this; }
    public Builder tenantId(String v) { this.tenantId = v; return this; }
    public Builder actor(Actor v) { this.actor = v; return this; }
    public Builder action(AuditAction v) { this.action = v; return this; }
    public Builder entity(String type, String id) { this.entityType = type; this.entityId = id; return this; }
    public Builder outcome(AuditOutcome v) { this.outcome = v; return this; }
    public Builder sensitivity(Sensitivity v) { this.sensitivity = v; return this; }
    public Builder dataClass(DataClass v) { this.dataClass = v; return this; }
    public Builder resource(String k, Object v) { this.resource.put(k, v); return this; }
    public Builder putResource(Map<String,Object> m) { this.resource.putAll(m); return this; }
    public Builder diff(String k, Object v) { this.diff.put(k, v); return this; }
    public Builder putDiff(Map<String,Object> m) { this.diff.putAll(m); return this; }
    public Builder meta(String k, Object v) { this.metadata.put(k, v); return this; }
    public Builder putMeta(Map<String,Object> m) { this.metadata.putAll(m); return this; }
    public Builder message(String v) { this.message = v; return this; }
    public Builder put(String k, Object v) { this.payload.put(k, v); return this; }
    public Builder putAll(Map<String,Object> m) { this.payload.putAll(m); return this; }
    public AuditEvent build() { return new AuditEvent(this); }
  }
}
