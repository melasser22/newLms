package com.ejada.catalog.config;

import com.ejada.common.events.provisioning.TenantProvisioningProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TenantProvisioningConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "app.tenant-provisioning")
    public TenantProvisioningProperties tenantProvisioningProperties() {
        return new TenantProvisioningProperties();
    }
}
