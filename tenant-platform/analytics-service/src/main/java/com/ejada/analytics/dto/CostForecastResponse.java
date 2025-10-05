package com.ejada.analytics.dto;

import java.math.BigDecimal;
import java.util.List;

public record CostForecastResponse(
    Long tenantId,
    List<FeatureForecastDto> features,
    OffsetDateTimeRange forecastWindow) {

  public CostForecastResponse(
      Long tenantId, List<FeatureForecastDto> features, OffsetDateTimeRange forecastWindow) {
    this.tenantId = tenantId;
    this.features = features == null ? List.of() : List.copyOf(features);
    this.forecastWindow = forecastWindow;
  }

  public record FeatureForecastDto(
      String featureKey,
      BigDecimal currentUsage,
      BigDecimal forecastedUsage,
      BigDecimal planLimit,
      boolean overageRisk,
      List<String> recommendations) {

    public FeatureForecastDto(
        String featureKey,
        BigDecimal currentUsage,
        BigDecimal forecastedUsage,
        BigDecimal planLimit,
        boolean overageRisk,
        List<String> recommendations) {
      this.featureKey = featureKey;
      this.currentUsage = currentUsage;
      this.forecastedUsage = forecastedUsage;
      this.planLimit = planLimit;
      this.overageRisk = overageRisk;
      this.recommendations =
          recommendations == null ? List.of() : List.copyOf(recommendations);
    }
  }

  public record OffsetDateTimeRange(java.time.OffsetDateTime start, java.time.OffsetDateTime end) {}
}
