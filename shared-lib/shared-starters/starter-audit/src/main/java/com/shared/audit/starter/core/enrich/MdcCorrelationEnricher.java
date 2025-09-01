package com.shared.audit.starter.core.enrich;

import com.shared.audit.starter.api.AuditEvent;
import com.common.constants.HeaderNames;
import com.common.context.CorrelationContextUtil;

public class MdcCorrelationEnricher implements Enricher {
  @Override public void enrich(AuditEvent.Builder b) {
    String correlationId = CorrelationContextUtil.getCorrelationId();
    b.meta(HeaderNames.CORRELATION_ID, correlationId);
  }
}
