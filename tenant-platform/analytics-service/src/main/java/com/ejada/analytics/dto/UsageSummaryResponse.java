package com.ejada.analytics.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record UsageSummaryResponse(
    Long tenantId,
    AnalyticsPeriod period,
    OffsetDateTime periodStart,
    OffsetDateTime periodEnd,
    List<FeatureUsageSummaryDto> features) {}
