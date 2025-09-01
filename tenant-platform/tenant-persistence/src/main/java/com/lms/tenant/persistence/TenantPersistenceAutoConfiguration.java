package com.lms.tenant.persistence;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Auto-configuration for tenant persistence components.
 */
@AutoConfiguration
@EntityScan(basePackages = "com.lms.tenant.persistence.entity")
@EnableJpaRepositories(basePackages = "com.lms.tenant.persistence.repository")
public class TenantPersistenceAutoConfiguration {
}

