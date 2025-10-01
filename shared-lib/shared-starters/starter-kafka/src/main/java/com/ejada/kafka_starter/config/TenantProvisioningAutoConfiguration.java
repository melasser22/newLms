package com.ejada.kafka_starter.config;

import com.ejada.common.events.provisioning.TenantProvisioningProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration that exposes the {@link TenantProvisioningProperties} bean when Kafka starter is used.
 */
@AutoConfiguration
public class TenantProvisioningAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(prefix = "app.tenant-provisioning")
    public TenantProvisioningProperties tenantProvisioningProperties() {
        return new TenantProvisioningProperties();
    }
}
