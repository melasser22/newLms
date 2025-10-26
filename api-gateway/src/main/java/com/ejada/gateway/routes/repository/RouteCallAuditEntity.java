package com.ejada.gateway.routes.repository;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table(value = "route_call_audit", schema = "public")
public class RouteCallAuditEntity {

  @Id
  @Column("call_id")
  private UUID callId;

  @Column("route_id")
  private UUID routeId;

  @Column("route_id_raw")
  private String routeIdRaw;

  private String method;

  private String path;

  @Column("status_code")
  private Integer statusCode;

  @Column("duration_ms")
  private Long durationMs;

  @Column("tenant_id")
  private String tenantId;

  @Column("correlation_id")
  private String correlationId;

  @Column("client_ip")
  private String clientIp;

  private String outcome;

  @Column("error_message")
  private String errorMessage;

  @Column("occurred_at")
  private Instant occurredAt;

  public UUID getCallId() {
    return callId;
  }

  public void setCallId(UUID callId) {
    this.callId = callId;
  }

  public UUID getRouteId() {
    return routeId;
  }

  public void setRouteId(UUID routeId) {
    this.routeId = routeId;
  }

  public String getRouteIdRaw() {
    return routeIdRaw;
  }

  public void setRouteIdRaw(String routeIdRaw) {
    this.routeIdRaw = routeIdRaw;
  }

  public String getMethod() {
    return method;
  }

  public void setMethod(String method) {
    this.method = method;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public Integer getStatusCode() {
    return statusCode;
  }

  public void setStatusCode(Integer statusCode) {
    this.statusCode = statusCode;
  }

  public Long getDurationMs() {
    return durationMs;
  }

  public void setDurationMs(Long durationMs) {
    this.durationMs = durationMs;
  }

  public String getTenantId() {
    return tenantId;
  }

  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }

  public String getCorrelationId() {
    return correlationId;
  }

  public void setCorrelationId(String correlationId) {
    this.correlationId = correlationId;
  }

  public String getClientIp() {
    return clientIp;
  }

  public void setClientIp(String clientIp) {
    this.clientIp = clientIp;
  }

  public String getOutcome() {
    return outcome;
  }

  public void setOutcome(String outcome) {
    this.outcome = outcome;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }

  public void setOccurredAt(Instant occurredAt) {
    this.occurredAt = occurredAt;
  }
}

