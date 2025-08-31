package com.shared.audit.starter.core.enrich;

import com.common.constants.HeaderNames;
import com.common.context.ContextManager;
import com.shared.audit.starter.api.AuditEvent;

/**
 * Adds standard header values from the {@link ContextManager} to audit metadata
 * so that audit records can be correlated with requests and users without
 * relying on duplicated MDC lookups.
 */
public class ContextHeaderEnricher implements Enricher {

  @Override
  public void enrich(AuditEvent.Builder b) {
    putMeta(b, HeaderNames.CORRELATION_ID, ContextManager.getCorrelationId());
    putMeta(b, HeaderNames.REQUEST_ID, ContextManager.getRequestId());
    String tenant = ContextManager.Tenant.get();
    if (tenant != null) {
      b.tenantId(tenant);
    }
    putMeta(b, HeaderNames.USER_ID, ContextManager.getUserId());
  }

  private static void putMeta(AuditEvent.Builder b, String key, String value) {
    if (value != null) {
      b.meta(key, value);
    }
  }
}
