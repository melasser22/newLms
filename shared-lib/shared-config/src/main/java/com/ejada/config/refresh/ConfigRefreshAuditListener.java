package com.ejada.config.refresh;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * Listens for configuration refresh events (triggered via /actuator/refresh)
 * and emits structured audit logs capturing the before/after values.
 */
public class ConfigRefreshAuditListener implements ApplicationListener<ApplicationEvent> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigRefreshAuditListener.class);
  private static final String ENVIRONMENT_CHANGE_EVENT_CLASS_NAME =
      "org.springframework.cloud.context.environment.EnvironmentChangeEvent";
  private static final Class<?> ENVIRONMENT_CHANGE_EVENT_CLASS = resolveEnvironmentChangeEventClass();
  private static final Method GET_KEYS_METHOD = resolveGetKeysMethod();

  private final ConfigurableEnvironment environment;
  private final ConfigVersionTracker versionTracker;
  private final Map<String, String> lastKnownValues = new ConcurrentHashMap<>();

  public ConfigRefreshAuditListener(ConfigurableEnvironment environment, ConfigVersionTracker versionTracker) {
    this.environment = environment;
    this.versionTracker = versionTracker;
  }

  @Override
  public void onApplicationEvent(ApplicationEvent event) {
    if (!isEnvironmentChangeEvent(event)) {
      return;
    }
    Collection<String> keys = extractChangedKeys(event);
    long version = versionTracker.incrementAndGet();
    keys.stream().sorted().forEach(key -> logChange(version, key));
    environment.getSystemProperties().put("app.configuration-version", Long.toString(version));
    if (keys.isEmpty()) {
      LOGGER.info("Configuration refresh #{}, no keys reported", version);
    } else {
      LOGGER.info("Configuration refresh #{} applied to {} keys", version, keys.size());
    }
  }

  private static Class<?> resolveEnvironmentChangeEventClass() {
    try {
      return ClassUtils.forName(ENVIRONMENT_CHANGE_EVENT_CLASS_NAME,
          ConfigRefreshAuditListener.class.getClassLoader());
    } catch (ClassNotFoundException ex) {
      return null;
    }
  }

  private static Method resolveGetKeysMethod() {
    if (ENVIRONMENT_CHANGE_EVENT_CLASS == null) {
      return null;
    }
    Method method = ReflectionUtils.findMethod(ENVIRONMENT_CHANGE_EVENT_CLASS, "getKeys");
    if (method != null) {
      ReflectionUtils.makeAccessible(method);
    }
    return method;
  }

  private boolean isEnvironmentChangeEvent(ApplicationEvent event) {
    return ENVIRONMENT_CHANGE_EVENT_CLASS != null && GET_KEYS_METHOD != null
        && ENVIRONMENT_CHANGE_EVENT_CLASS.isInstance(event);
  }

  @SuppressWarnings("unchecked")
  private Collection<String> extractChangedKeys(ApplicationEvent event) {
    if (GET_KEYS_METHOD == null) {
      return Collections.emptySet();
    }
    Object keys = ReflectionUtils.invokeMethod(GET_KEYS_METHOD, event);
    if (keys instanceof Collection<?>) {
      return (Collection<String>) keys;
    }
    return Collections.emptySet();
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
