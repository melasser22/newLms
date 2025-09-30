package com.ejada.gateway.fallback;

import java.time.Instant;

/**
 * Simple payload returned to clients when a downstream service is unavailable
 * and the gateway falls back locally.
 */
public record FallbackResponse(String routeId, String message, Instant timestamp) {
}
