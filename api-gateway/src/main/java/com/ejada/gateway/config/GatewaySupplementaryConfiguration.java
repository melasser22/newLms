package com.ejada.gateway.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Enables auxiliary configuration properties for gateway-specific components
 * that are not part of the shared starters.
 */
@Configuration
@EnableConfigurationProperties({SubscriptionValidationProperties.class, AdminAggregationProperties.class})
public class GatewaySupplementaryConfiguration {
}

