package com.ejada.gateway.metrics;

/**
 * View model representing gateway request volume for a tenant within the
 * configured observation window.
 */
public record TenantRequestMetric(String tenantId, long requestCount) { }
