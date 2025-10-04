package com.ejada.gateway.bff;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

/**
 * Aggregated payload returned by the tenant dashboard BFF endpoint. The
 * payload intentionally surfaces downstream JSON nodes so the UI can evolve
 * without recompiling the gateway while warnings communicate any partial
 * failures handled gracefully by the aggregator.
 */
public record TenantDashboardResponse(
    JsonNode tenant,
    JsonNode usageSummary,
    JsonNode featureAdoption,
    JsonNode costForecast,
    JsonNode consumption,
    List<String> warnings) {

  public TenantDashboardResponse {
    warnings = (warnings == null) ? List.of() : List.copyOf(warnings);
  }
}
