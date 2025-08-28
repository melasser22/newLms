package com.shared.kafka_starter.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;

public class KafkaObservabilityConfig {

  @Bean
  @ConditionalOnBean(MeterRegistry.class)
  public MeterRegistryCustomizer<MeterRegistry> meterCommonTags() {
    return registry -> registry.config().commonTags("component", "kafka");
  }
}
