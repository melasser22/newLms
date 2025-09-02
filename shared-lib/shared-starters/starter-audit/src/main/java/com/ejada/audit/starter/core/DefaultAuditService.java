package com.ejada.audit.starter.core;

import com.ejada.audit.starter.api.AuditEvent;
import com.ejada.audit.starter.api.AuditService;
import com.ejada.audit.starter.core.dispatch.AuditDispatcher;
import com.ejada.audit.starter.core.enrich.Enricher;
import com.ejada.audit.starter.core.mask.MaskingStrategy;

import java.util.List;
import java.util.Map;

public class DefaultAuditService implements AuditService {
  private final AuditDispatcher dispatcher;
  private final List<Enricher> enrichers;
  private final MaskingStrategy masking;

  public DefaultAuditService(AuditDispatcher dispatcher, List<Enricher> enrichers, MaskingStrategy masking) {
    this.dispatcher = dispatcher; this.enrichers = enrichers; this.masking = masking;
  }

  @Override public void emit(AuditEvent event) {
    AuditEvent.Builder b = AuditEvent.builder()
        .schemaVersion(event.getSchemaVersion())
        .eventId(event.getEventId())
        .timestamp(event.getTimestamp())
        .tenantId(event.getTenantId())
        .actor(event.getActor())
        .action(event.getAction())
        .entity(event.getEntityType(), event.getEntityId())
        .outcome(event.getOutcome())
        .sensitivity(event.getSensitivity())
        .dataClass(event.getDataClass())
        .putResource(event.getResource())
        .putDiff(event.getDiff())
        .putMeta(event.getMetadata())
        .message(event.getMessage())
        .putAll(event.getPayload());

    for (Enricher e : enrichers) e.enrich(b);

    // apply masking (if diff exists)
    Map<String, Object> masked = masking.mask(event.getEntityType(), event.getDiff(), event.getDiff());
    b.putDiff(masked);

    dispatcher.dispatch(b.build());
  }
}
