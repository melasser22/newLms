package com.lms.tenant.events.core;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Persisted envelope for publishing; includes headers (e.g., tenant_id, trace_id). */
public record EventEnvelope(
    UUID eventId,
    String eventType,
    String aggregateType,
    UUID aggregateId,
    Instant occurredAt,
    Map<String, Object> payload,
    Map<String, Object> headers
) {}
