package com.ejada.sec.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sec.kafka.topics")
public record SecKafkaTopicsProperties(String tenantOnboarding) {

    public SecKafkaTopicsProperties {
        if (tenantOnboarding == null || tenantOnboarding.isBlank()) {
            throw new IllegalArgumentException("sec.kafka.topics.tenant-onboarding must be configured");
        }
    }
}
