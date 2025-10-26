package com.ejada.gateway.routes.model;

/**
 * Immutable representation of a gateway route call used for persistence in the audit trail.
 */
public record RouteCallAuditRecord(
    String routeId,
    String path,
    String method,
    int statusCode,
    long durationMs,
    String tenantId,
    String correlationId,
    String clientIp,
    String outcome,
    String errorMessage) {}

