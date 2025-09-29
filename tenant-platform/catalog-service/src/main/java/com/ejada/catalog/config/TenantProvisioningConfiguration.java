package com.ejada.catalog.config;

import com.ejada.common.events.provisioning.TenantProvisioningProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TenantProvisioningProperties.class)
public class TenantProvisioningConfiguration {
}
