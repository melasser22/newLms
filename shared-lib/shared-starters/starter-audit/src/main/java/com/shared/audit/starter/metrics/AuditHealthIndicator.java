package com.shared.audit.starter.metrics;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

public class AuditHealthIndicator implements HealthIndicator {
  @Override public Health health() {
    return Health.up().withDetail("audit","ok").build();
  }
}
