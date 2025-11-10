package com.ejada.tenant.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tenant.kafka.topics")
public record TenantKafkaTopicsProperties(String tenantOnboarding) {

    public TenantKafkaTopicsProperties {
        if (tenantOnboarding == null || tenantOnboarding.isBlank()) {
            throw new IllegalArgumentException("tenant.kafka.topics.tenant-onboarding must be configured");
        }
    }
}
