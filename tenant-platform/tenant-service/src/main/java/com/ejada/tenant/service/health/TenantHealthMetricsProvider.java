package com.ejada.tenant.service.health;

public interface TenantHealthMetricsProvider {

    TenantHealthMetrics collect(Integer tenantId);
}
