package com.shared.audit.starter.core.enrich;

import com.shared.audit.starter.api.AuditEvent;
import com.common.context.CorrelationContextUtil;

import org.slf4j.MDC;

public class MdcCorrelationEnricher implements Enricher {
  @Override public void enrich(AuditEvent.Builder b) {
    String correlationId = CorrelationContextUtil.getCorrelationId();
    String spanId = MDC.get("spanId");
    b.meta("correlationId", correlationId);

    if (spanId != null) b.meta("spanId", spanId);
  }
}
