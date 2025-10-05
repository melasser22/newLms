package com.ejada.analytics.dto;

import java.math.BigDecimal;
import java.util.List;

public record FeatureUsageSummaryDto(
    String featureKey,
    BigDecimal totalUsage,
    Long eventCount,
    BigDecimal planLimit,
    BigDecimal utilizationPercentage,
    BigDecimal forecastedUsage,
    boolean predictedOverage,
    List<String> costRecommendations,
    List<PeakUsageWindowDto> peakUsageWindows) {

  public FeatureUsageSummaryDto(
      String featureKey,
      BigDecimal totalUsage,
      Long eventCount,
      BigDecimal planLimit,
      BigDecimal utilizationPercentage,
      BigDecimal forecastedUsage,
      boolean predictedOverage,
      List<String> costRecommendations,
      List<PeakUsageWindowDto> peakUsageWindows) {
    this.featureKey = featureKey;
    this.totalUsage = totalUsage;
    this.eventCount = eventCount;
    this.planLimit = planLimit;
    this.utilizationPercentage = utilizationPercentage;
    this.forecastedUsage = forecastedUsage;
    this.predictedOverage = predictedOverage;
    this.costRecommendations =
        costRecommendations == null ? List.of() : List.copyOf(costRecommendations);
    this.peakUsageWindows = peakUsageWindows == null ? List.of() : List.copyOf(peakUsageWindows);
  }
}
