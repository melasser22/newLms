package com.shared.audit.starter.core.enrich;

import com.shared.audit.starter.api.AuditEvent;
import org.slf4j.MDC;

public class MdcCorrelationEnricher implements Enricher {
  @Override public void enrich(AuditEvent.Builder b) {
    String correlationId = MDC.get("correlationId");
    String spanId = MDC.get("spanId");
    if (correlationId != null) b.meta("correlationId", correlationId);
    if (spanId != null) b.meta("spanId", spanId);
  }
}
