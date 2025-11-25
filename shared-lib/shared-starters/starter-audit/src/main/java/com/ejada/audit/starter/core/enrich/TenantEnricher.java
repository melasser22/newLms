package com.ejada.audit.starter.core.enrich;

import com.ejada.audit.starter.api.AuditEvent;

/** Uses a ThreadLocal or external tenant supplier if available. */
public class TenantEnricher implements Enricher {
  private final java.util.function.Supplier<String> tenantSupplier;
  public TenantEnricher(java.util.function.Supplier<String> supplier) { this.tenantSupplier = supplier; }
  @Override public void enrich(AuditEvent.Builder b) {
    String t = tenantSupplier.get();
    if (t != null && !t.isBlank()) b.tenantId(t);
  }
}
