package com.ejada.gateway.fallback;

import java.time.Instant;

/**
 * Metadata returned to clients when billing usage events are queued because
 * the downstream billing service is unavailable.
 */
public record BillingFallbackPayload(
    String queueKey,
    Instant queuedAt,
    String path,
    String method) {
}
