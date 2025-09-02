package com.ejada.audit.starter.core.enrich;

import com.ejada.audit.starter.api.AuditEvent;
import com.ejada.common.constants.HeaderNames;
import com.ejada.common.context.CorrelationContextUtil;

public class MdcCorrelationEnricher implements Enricher {
  @Override public void enrich(AuditEvent.Builder b) {
    String correlationId = CorrelationContextUtil.getCorrelationId();
    b.meta(HeaderNames.CORRELATION_ID, correlationId);
  }
}
