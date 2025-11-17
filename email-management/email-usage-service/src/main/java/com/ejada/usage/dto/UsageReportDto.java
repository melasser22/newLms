package com.ejada.usage.dto;

import java.util.List;

public record UsageReportDto(String tenantId, List<UsageMetricDto> metrics) {

  public UsageReportDto {
    this.metrics = metrics == null ? List.of() : List.copyOf(metrics);
  }

  @Override
  public List<UsageMetricDto> metrics() {
    return List.copyOf(metrics);
  }
}
