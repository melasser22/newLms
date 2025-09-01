package com.lms.tenant.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Instant;
import java.util.Map;

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
