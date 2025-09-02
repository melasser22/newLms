package com.ejada.tenant.core;

import java.time.Instant;
import java.util.UUID;

public interface SubscriptionPort {
    ActiveSubscription activeSubscription(UUID tenantId);

    record ActiveSubscription(UUID subscriptionId,
                              String tierId,
                              Instant periodStart,
                              Instant periodEnd) {
    }
}
