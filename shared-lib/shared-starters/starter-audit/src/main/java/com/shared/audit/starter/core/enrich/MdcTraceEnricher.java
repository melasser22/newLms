package com.shared.audit.starter.core.enrich;

import com.shared.audit.starter.api.AuditEvent;
import org.slf4j.MDC;

public class MdcTraceEnricher implements Enricher {
  @Override public void enrich(AuditEvent.Builder b) {
    String traceId = MDC.get("traceId");
    String spanId = MDC.get("spanId");
    if (traceId != null) b.meta("traceId", traceId);
    if (spanId != null) b.meta("spanId", spanId);
  }
}
