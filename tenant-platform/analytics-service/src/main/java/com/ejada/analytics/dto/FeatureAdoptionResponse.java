package com.ejada.analytics.dto;

import java.util.List;

public record FeatureAdoptionResponse(Long tenantId, List<FeatureTrendDto> features) {

  public record FeatureTrendDto(String featureKey, List<UsageTrendPointDto> trend) {}
}
