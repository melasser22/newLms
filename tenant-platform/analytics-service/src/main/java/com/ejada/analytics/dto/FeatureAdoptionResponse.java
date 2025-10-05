package com.ejada.analytics.dto;

import java.util.List;

public record FeatureAdoptionResponse(Long tenantId, List<FeatureTrendDto> features) {

  public FeatureAdoptionResponse(Long tenantId, List<FeatureTrendDto> features) {
    this.tenantId = tenantId;
    this.features = features == null ? List.of() : List.copyOf(features);
  }

  public record FeatureTrendDto(String featureKey, List<UsageTrendPointDto> trend) {

    public FeatureTrendDto(String featureKey, List<UsageTrendPointDto> trend) {
      this.featureKey = featureKey;
      this.trend = trend == null ? List.of() : List.copyOf(trend);
    }
  }
}
