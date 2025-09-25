package com.ejada.actuator.starter.health;

import com.ejada.actuator.starter.config.SharedActuatorProperties;
import java.time.Clock;
import java.time.OffsetDateTime;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

public class SlaHealthIndicator extends AbstractHealthIndicator {

  private final SharedActuatorProperties properties;
  private final Clock clock;

  public SlaHealthIndicator(SharedActuatorProperties properties) {
    this(properties, Clock.systemUTC());
  }

  SlaHealthIndicator(SharedActuatorProperties properties, Clock clock) {
    this.properties = properties;
    this.clock = clock;
  }

  @Override
  protected void doHealthCheck(Health.Builder builder) {
    SharedActuatorProperties.SlaReport sla = properties.getSlaReport();
    builder.status(Status.UP)
        .withDetail("sla_compliant", sla.isSlaCompliant())
        .withDetail("availability_percent", sla.getAvailabilityPercent())
        .withDetail("last_check", OffsetDateTime.now(clock));
  }
}
