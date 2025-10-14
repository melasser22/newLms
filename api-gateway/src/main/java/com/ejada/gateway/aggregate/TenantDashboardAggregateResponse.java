package com.ejada.gateway.aggregate;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.List;

/**
 * Immutable response describing the aggregated tenant dashboard returned by the aggregation
 * endpoints. Exposes downstream JSON payloads verbatim while normalising meta such as warnings
 * and timestamp.
 */
public record TenantDashboardAggregateResponse(JsonNode tenant,
                                               JsonNode subscriptions,
                                               JsonNode billing,
                                               List<String> warnings,
                                               Instant generatedAt) {

  public TenantDashboardAggregateResponse {
    warnings = (warnings == null) ? List.of() : List.copyOf(warnings);
  }
}
