package com.ejada.gateway.admin;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.List;

/**
 * Composite admin payload bundling tenant profile, subscriptions, analytics usage statistics and
 * audit events for operator dashboards.
 */
public record AdminTenantAggregateResponse(JsonNode tenant,
                                           JsonNode subscriptions,
                                           JsonNode usage,
                                           JsonNode auditEvents,
                                           List<String> warnings,
                                           Instant generatedAt) {

  public AdminTenantAggregateResponse {
    warnings = (warnings == null) ? List.of() : List.copyOf(warnings);
  }
}
