package com.ejada.analytics.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record UsageSummaryResponse(
    Long tenantId,
    AnalyticsPeriod period,
    OffsetDateTime periodStart,
    OffsetDateTime periodEnd,
    List<FeatureUsageSummaryDto> features) {

  public UsageSummaryResponse(
      Long tenantId,
      AnalyticsPeriod period,
      OffsetDateTime periodStart,
      OffsetDateTime periodEnd,
      List<FeatureUsageSummaryDto> features) {
    this.tenantId = tenantId;
    this.period = period;
    this.periodStart = periodStart;
    this.periodEnd = periodEnd;
    this.features = features == null ? List.of() : List.copyOf(features);
  }
}
