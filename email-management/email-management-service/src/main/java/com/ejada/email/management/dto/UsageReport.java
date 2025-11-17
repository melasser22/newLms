package com.ejada.email.management.dto;

import java.util.List;

public record UsageReport(String tenantId, List<UsageMetric> metrics) {

  public UsageReport {
    metrics = metrics == null ? List.of() : List.copyOf(metrics);
  }

  @Override
  public List<UsageMetric> metrics() {
    return List.copyOf(metrics);
  }
}
