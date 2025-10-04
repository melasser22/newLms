package com.ejada.gateway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Enables auxiliary configuration properties for gateway-specific components
 * that are not part of the shared starters.
 */
@Configuration
@EnableConfigurationProperties({SubscriptionValidationProperties.class, GatewayBffProperties.class,AdminAggregationProperties.class})
public class GatewaySupplementaryConfiguration {
}
