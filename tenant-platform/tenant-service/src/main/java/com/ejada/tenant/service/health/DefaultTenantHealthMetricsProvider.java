package com.ejada.tenant.service.health;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultTenantHealthMetricsProvider implements TenantHealthMetricsProvider {

    @Override
    public TenantHealthMetrics collect(final Integer tenantId) {
        log.debug("Using default tenant health metrics provider for tenant {}", tenantId);
        return TenantHealthMetrics.empty();
    }
}
