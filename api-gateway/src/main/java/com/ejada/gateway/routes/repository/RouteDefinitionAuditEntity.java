package com.ejada.gateway.routes.repository;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("route_definition_audit")
public class RouteDefinitionAuditEntity {

  @Id
  @Column("audit_id")
  private UUID auditId;

  @Column("route_id")
  private UUID routeId;

  @Column("change_type")
  private String changeType;

  private String payload;

  @Column("changed_by")
  private String changedBy;

  @Column("changed_at")
  private Instant changedAt;

  private int version;

  public UUID getAuditId() {
    return auditId;
  }

  public void setAuditId(UUID auditId) {
    this.auditId = auditId;
  }

  public UUID getRouteId() {
    return routeId;
  }

  public void setRouteId(UUID routeId) {
    this.routeId = routeId;
  }

  public String getChangeType() {
    return changeType;
  }

  public void setChangeType(String changeType) {
    this.changeType = changeType;
  }

  public String getPayload() {
    return payload;
  }

  public void setPayload(String payload) {
    this.payload = payload;
  }

  public String getChangedBy() {
    return changedBy;
  }

  public void setChangedBy(String changedBy) {
    this.changedBy = changedBy;
  }

  public Instant getChangedAt() {
    return changedAt;
  }

  public void setChangedAt(Instant changedAt) {
    this.changedAt = changedAt;
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }
}
