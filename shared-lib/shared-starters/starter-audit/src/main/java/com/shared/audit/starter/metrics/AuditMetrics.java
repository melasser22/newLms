package com.shared.audit.starter.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

public class AuditMetrics {
  private final Counter produced;
  public AuditMetrics(MeterRegistry reg) {
    this.produced = Counter.builder("audit.events.produced.total").register(reg);
  }
  public void incProduced() { produced.increment(); }
}
