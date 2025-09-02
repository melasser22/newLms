package com.ejada.policy;

import java.time.Instant;
import java.util.UUID;

@FunctionalInterface
public interface UsageReader {
    long currentUsage(UUID tenantId, String featureKey, Instant periodStart, Instant periodEnd);
}
