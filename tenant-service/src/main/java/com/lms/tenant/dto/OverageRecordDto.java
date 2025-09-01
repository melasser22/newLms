package com.lms.tenant.dto;

import java.time.Instant;
import java.util.UUID;

public record OverageRecordDto(UUID id, UUID tenantId, String featureKey, long quantity,
                               long unitPriceMinor, String currency, Instant periodStart, Instant periodEnd) {
}
