package com.ejada.tenant.dto;

import com.ejada.tenant.model.TenantHealthRiskCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Schema(name = "TenantHealthScoreRes")
public record TenantHealthScoreRes(
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
) { }
