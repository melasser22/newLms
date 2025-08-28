
package com.shared.actuator.starter.metrics;

import com.shared.actuator.starter.config.SharedActuatorProperties;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.core.env.Environment;

public class CommonTagsCustomizer implements MeterRegistryCustomizer<MeterRegistry> {

  private final Environment env;
  private final SharedActuatorProperties props;

  public CommonTagsCustomizer(Environment env, SharedActuatorProperties props) {
    this.env = env;
    this.props = props;
  }

  @Override
  public void customize(MeterRegistry registry) {
    var tags = new java.util.ArrayList<String>();
    var common = props.getMetrics().getCommonTags();
    if (common.isApplicationEnabled()) {
      tags.add("application"); tags.add(env.getProperty("spring.application.name", "app"));
    }
    if (common.isEnvironmentEnabled()) {
      tags.add("env"); tags.add(env.getProperty("ENV", env.getProperty("SPRING_PROFILES_ACTIVE", "default")));
    }
    if (common.isRegionEnabled()) {
      tags.add("region"); tags.add(env.getProperty("REGION", "unknown"));
    }
    if (common.isZoneEnabled()) {
      tags.add("zone"); tags.add(env.getProperty("ZONE", "unknown"));
    }
    if (!tags.isEmpty()) {
      registry.config().commonTags(tags.toArray(String[]::new));
    }
  }
}
