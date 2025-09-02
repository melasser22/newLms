package com.ejada.audit.starter.core.enrich;

import com.ejada.audit.starter.api.AuditEvent;

public interface Enricher {
  void enrich(AuditEvent.Builder b);
}
