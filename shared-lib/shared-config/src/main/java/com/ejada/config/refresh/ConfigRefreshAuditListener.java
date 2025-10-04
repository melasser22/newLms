package com.ejada.config.refresh;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.EnvironmentChangeEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.StringUtils;

/**
 * Listens for configuration refresh events (triggered via /actuator/refresh)
 * and emits structured audit logs capturing the before/after values.
 */
public class ConfigRefreshAuditListener implements ApplicationListener<EnvironmentChangeEvent> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigRefreshAuditListener.class);

  private final ConfigurableEnvironment environment;
  private final ConfigVersionTracker versionTracker;
  private final Map<String, String> lastKnownValues = new ConcurrentHashMap<>();

  public ConfigRefreshAuditListener(ConfigurableEnvironment environment, ConfigVersionTracker versionTracker) {
    this.environment = environment;
    this.versionTracker = versionTracker;
  }

  @Override
  public void onApplicationEvent(EnvironmentChangeEvent event) {
    long version = versionTracker.incrementAndGet();
    event.getKeys().stream().sorted().forEach(key -> logChange(version, key));
    environment.getSystemProperties().put("app.configuration-version", Long.toString(version));
    if (event.getKeys().isEmpty()) {
      LOGGER.info("Configuration refresh #{}, no keys reported", version);
    } else {
      LOGGER.info("Configuration refresh #{} applied to {} keys", version, event.getKeys().size());
    }
  }

  private void logChange(long version, String key) {
    String normalized = (key == null) ? "" : key.toLowerCase(Locale.ROOT);
    String newValue = environment.getProperty(key);
    String previousValue = lastKnownValues.put(key, newValue);
    LOGGER.info("Configuration refresh #{} -> {}: {} -> {}", version, key, mask(normalized, previousValue),
        mask(normalized, newValue));
  }

  private String mask(String key, String value) {
    if (!StringUtils.hasText(value)) {
      return value;
    }
    if (key.contains("password") || key.contains("secret") || key.contains("token")) {
      return "****";
    }
    return value;
  }
}
