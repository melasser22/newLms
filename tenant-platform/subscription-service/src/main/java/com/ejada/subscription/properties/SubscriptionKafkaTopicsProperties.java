package com.ejada.subscription.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "subscription.kafka.topics")
public record SubscriptionKafkaTopicsProperties(String tenantOnboarding) {

    public SubscriptionKafkaTopicsProperties {
        if (tenantOnboarding == null || tenantOnboarding.isBlank()) {
            throw new IllegalArgumentException("subscription.kafka.topics.tenant-onboarding must be configured");
        }
    }
}
