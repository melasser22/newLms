package com.ejada.tenant.dto.event;

import com.ejada.tenant.model.TenantHealthRiskCategory;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TenantHealthScoreCalculatedEvent(
        Integer tenantId,
        Integer score,
        TenantHealthRiskCategory riskCategory,
        BigDecimal featureAdoptionRate,
        BigDecimal loginFrequencyScore,
        BigDecimal userEngagementScore,
        BigDecimal usageTrendPercent,
        BigDecimal supportTicketScore,
        BigDecimal paymentHistoryScore,
        BigDecimal apiHealthScore,
        OffsetDateTime evaluatedAt
) {
}
