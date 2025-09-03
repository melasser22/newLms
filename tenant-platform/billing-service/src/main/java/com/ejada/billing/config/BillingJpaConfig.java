package com.ejada.billing.config;

import com.ejada.billing.entity.TenantOverage;
import com.ejada.billing.repository.TenantOverageRepository;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Configures JPA components for the billing service.
 *
 * <p>Adds package scanning for billing entities and repositories so that
 * {@link TenantOverageRepository} is registered alongside repositories
 * imported by other modules.</p>
 */
@Configuration
@EnableJpaRepositories(basePackageClasses = TenantOverageRepository.class)
@EntityScan(basePackageClasses = TenantOverage.class)
public class BillingJpaConfig {
}

