package com.shared.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Auto-configuration that exposes {@link EnvironmentProperties} so applications can centralize
 * environment and version information.
 */
@AutoConfiguration
@EnableConfigurationProperties(EnvironmentProperties.class)
public class EnvironmentAutoConfiguration {
}
