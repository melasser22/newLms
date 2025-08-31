package com.shared.kafka_starter.core;
import java.time.Instant;

public record EventEnvelope<T>(
    String event,       // e.g. points.earned
    Instant timestamp,  // event time
    String tenantId,
    String correlationId,
    String traceId,
    String schemaVersion,
    T data
) {}