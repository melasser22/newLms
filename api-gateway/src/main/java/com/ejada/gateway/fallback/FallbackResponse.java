package com.ejada.gateway.fallback;

import java.time.Instant;
import java.util.Map;

/**
 * Payload returned to clients when a downstream service is unavailable and the
 * gateway falls back locally. The payload now carries optional contextual data
 * and metadata so individual fallbacks can expose cached artefacts or
 * degradation hints to the caller.
 */
public record FallbackResponse(
    String routeId,
    String message,
    Instant timestamp,
    Object data,
    Map<String, Object> metadata) {

  public FallbackResponse {
    metadata = (metadata == null) ? Map.of() : Map.copyOf(metadata);
  }
}
