package com.lms.tenant.events.tenants;

import com.lms.tenant.events.core.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/** Event emitted when a tenant's overage policy is toggled. */
public record OverageToggleChanged(UUID aggregateId,
                                   boolean enabled,
                                   Instant occurredAt) implements DomainEvent {

  public OverageToggleChanged(UUID tenantId, boolean enabled) {
    this(tenantId, enabled, Instant.now());
  }

  @Override
  public String eventType() {
    return "OverageToggleChanged";
  }

  @Override
  public String aggregateType() {
    return "tenant";
  }
}
