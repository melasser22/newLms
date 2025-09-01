package com.lms.tenant.events.core;

import java.time.Instant;
import java.util.UUID;

/** Marker for domain events. Keep flat, JSON-friendly fields in implementations. */
public interface DomainEvent {
  String eventType();            // e.g. TenantCreated
  UUID aggregateId();            // e.g. tenantId
  String aggregateType();        // e.g. tenant
  Instant occurredAt();
}
