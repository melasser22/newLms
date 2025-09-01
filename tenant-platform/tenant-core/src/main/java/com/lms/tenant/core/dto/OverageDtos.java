package com.lms.tenant.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record RecordOverageRequest(
        @NotBlank String featureKey,
        @Positive long quantity,
        @PositiveOrZero Long unitPriceMinor,
        @NotBlank String currency,
        Instant occurredAt,
        @NotNull Instant periodStart,
        @NotNull Instant periodEnd,
        String idempotencyKey,
        Map<String, Object> metadata) {
}

public record OverageResponse(
        UUID overageId,
        UUID tenantId,
        String featureKey,
        long quantity,
        long unitPriceMinor,
        String currency,
        Instant occurredAt,
        Instant periodStart,
        Instant periodEnd,
        String status) {
}
