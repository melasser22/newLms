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
    List<PeakUsageWindowDto> peakUsageWindows) {}
