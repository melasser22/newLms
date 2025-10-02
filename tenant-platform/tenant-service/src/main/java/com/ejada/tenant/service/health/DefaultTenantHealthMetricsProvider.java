package com.ejada.tenant.service.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnMissingBean(TenantHealthMetricsProvider.class)
public class DefaultTenantHealthMetricsProvider implements TenantHealthMetricsProvider {

    @Override
    public TenantHealthMetrics collect(final Integer tenantId) {
        log.debug("Using default tenant health metrics provider for tenant {}", tenantId);
        return TenantHealthMetrics.empty();
    }
}
