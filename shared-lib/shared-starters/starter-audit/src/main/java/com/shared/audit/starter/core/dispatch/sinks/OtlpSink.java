package com.shared.audit.starter.core.dispatch.sinks;

import com.shared.audit.starter.api.AuditEvent;

/** Placeholder for OTLP exporter wiring via OpenTelemetry SDK */
public class OtlpSink implements Sink {
  private final String endpoint;

  public OtlpSink(String endpoint) {
    this.endpoint = endpoint;
  }

  @Override
  public void send(AuditEvent event) {
    // no-op demo
  }
}
