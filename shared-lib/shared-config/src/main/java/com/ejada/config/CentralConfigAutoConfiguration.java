package com.ejada.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Auto-configuration that exposes {@link AppProperties} for centralized
 * configuration management. Services can import this starter to consolidate
 * environment settings.
 */
@AutoConfiguration
@EnableConfigurationProperties(AppProperties.class)
public class CentralConfigAutoConfiguration {
}
