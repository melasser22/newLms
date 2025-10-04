package com.ejada.gateway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Registers configuration properties for the GraphQL gateway features. */
@Configuration
@EnableConfigurationProperties(GatewayGraphqlProperties.class)
public class GraphqlGatewayConfiguration {
}

