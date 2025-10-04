package com.ejada.tenant.config;

import com.ejada.tenant.service.health.DefaultTenantHealthMetricsProvider;
import com.ejada.tenant.service.health.TenantHealthMetricsProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class TenantHealthMetricsConfiguration {

    @Bean
    @ConditionalOnMissingBean(TenantHealthMetricsProvider.class)
    public TenantHealthMetricsProvider tenantHealthMetricsProvider() {
        return new DefaultTenantHealthMetricsProvider();
    }
}
