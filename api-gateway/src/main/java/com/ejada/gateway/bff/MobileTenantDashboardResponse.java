package com.ejada.gateway.bff;

import java.util.List;

/**
 * Mobile optimised dashboard payload containing a curated subset of metrics designed to minimise
 * payload size and render efficiently on constrained devices.
 */
public record MobileTenantDashboardResponse(String tenantId,
                                            String tenantName,
                                            String plan,
                                            String status,
                                            MobileBillingSummary billing,
                                            List<MobileMetric> highlights,
                                            List<String> warnings) {

  public MobileTenantDashboardResponse {
    highlights = (highlights == null) ? List.of() : List.copyOf(highlights);
    warnings = (warnings == null) ? List.of() : List.copyOf(warnings);
  }

  public record MobileBillingSummary(String currency, double monthlySpend, double usage) {
  }

  public record MobileMetric(String label, String value, String trend) {
  }
}
