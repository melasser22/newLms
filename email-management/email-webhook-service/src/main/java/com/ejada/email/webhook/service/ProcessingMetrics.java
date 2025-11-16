package com.ejada.email.webhook.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class ProcessingMetrics {

  private final MeterRegistry meterRegistry;
  private final Counter processed;
  private final Counter duplicates;
  private final Counter failures;
  private final Map<String, Counter> typeCounters = new ConcurrentHashMap<>();

  public ProcessingMetrics(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
    this.processed = meterRegistry.counter("sendgrid.webhook.processed");
    this.duplicates = meterRegistry.counter("sendgrid.webhook.duplicates");
    this.failures = meterRegistry.counter("sendgrid.webhook.failures");
  }

  public void incrementProcessed() {
    processed.increment();
  }

  public void incrementDuplicate() {
    duplicates.increment();
  }

  public void incrementFailure() {
    failures.increment();
  }

  public void incrementType(String type) {
    typeCounters
        .computeIfAbsent(
            type,
            key -> Counter.builder("sendgrid.webhook.type").tag("type", key).register(meterRegistry))
        .increment();
  }
}
