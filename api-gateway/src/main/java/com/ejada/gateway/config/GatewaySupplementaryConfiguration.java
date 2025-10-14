package com.ejada.gateway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables auxiliary configuration properties for gateway-specific components
 * that are not part of the shared starters.
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties({SubscriptionValidationProperties.class,
    SubscriptionWarmupProperties.class,
    GatewayBffProperties.class,
    AdminAggregationProperties.class,
    GatewayLoggingProperties.class,
    GatewayTracingProperties.class})
public class GatewaySupplementaryConfiguration {
}
