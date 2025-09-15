package com.ejada.audit.starter.core.dispatch.sinks;

import com.ejada.audit.starter.api.AuditEvent;

/** Placeholder for OTLP exporter wiring via OpenTelemetry SDK */
public class OtlpSink implements Sink {
  @SuppressWarnings("unused")
  private final String endpoint;

  public OtlpSink(String endpoint) {
    this.endpoint = endpoint;
  }

  @Override
  public void send(AuditEvent event) {
    // no-op demo
  }
}
