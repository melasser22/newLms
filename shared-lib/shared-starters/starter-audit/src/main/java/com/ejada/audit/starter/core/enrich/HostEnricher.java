package com.ejada.audit.starter.core.enrich;

import com.ejada.audit.starter.api.AuditEvent;

public class HostEnricher implements Enricher {
  private final String host = System.getenv().getOrDefault("HOSTNAME", "unknown");
  private final String pod = System.getenv().getOrDefault("POD_NAME", "unknown");
  private final String node = System.getenv().getOrDefault("NODE_NAME", "unknown");
  private final String region = System.getenv().getOrDefault("REGION", "unknown");
  @Override public void enrich(AuditEvent.Builder b) {
    b.meta("host", host);
    b.meta("pod", pod);
    b.meta("node", node);
    b.meta("region", region);
  }
}
