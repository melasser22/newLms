package com.ejada.config;

import com.ejada.config.refresh.ConfigRefreshAuditListener;
import com.ejada.config.refresh.ConfigVersionTracker;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Auto-configuration that exposes {@link AppProperties} for centralized
 * configuration management. Services can import this starter to consolidate
 * environment settings.
 */
@AutoConfiguration
@EnableConfigurationProperties(AppProperties.class)
public class CentralConfigAutoConfiguration {

  @Bean
  public ConfigVersionTracker configVersionTracker(ConfigurableEnvironment environment) {
    ConfigVersionTracker tracker = new ConfigVersionTracker();
    environment.getSystemProperties().putIfAbsent("app.configuration-version",
        Long.toString(tracker.getCurrentVersion()));
    return tracker;
  }

  @Bean
  public ConfigRefreshAuditListener configRefreshAuditListener(ConfigurableEnvironment environment,
      ConfigVersionTracker configVersionTracker) {
    return new ConfigRefreshAuditListener(environment, configVersionTracker);
  }
}
