package com.shared.audit.starter.core.enrich;

import com.shared.audit.starter.api.AuditEvent;
import org.slf4j.MDC;
import com.common.constants.HeaderNames;

/**
 * Adds standard header values from the SLF4J MDC to audit metadata so that
 * audit records can be correlated with requests and users.
 */
public class MdcHeaderEnricher implements Enricher {
  @Override
  public void enrich(AuditEvent.Builder b) {
    putMeta(b, HeaderNames.CORRELATION_ID, MDC.get(HeaderNames.CORRELATION_ID));
    putMeta(b, HeaderNames.REQUEST_ID, MDC.get(HeaderNames.REQUEST_ID));
    String tenant = MDC.get(HeaderNames.TENANT_ID);
    if (tenant != null) b.tenantId(tenant);
    putMeta(b, HeaderNames.USER_ID, MDC.get(HeaderNames.USER_ID));
  }

  private static void putMeta(AuditEvent.Builder b, String key, String value) {
    if (value != null) {
      b.meta(key, value);
    }
  }
}
