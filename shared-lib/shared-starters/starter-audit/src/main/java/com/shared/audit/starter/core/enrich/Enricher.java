package com.shared.audit.starter.core.enrich;

import com.shared.audit.starter.api.AuditEvent;

public interface Enricher {
  void enrich(AuditEvent.Builder b);
}
