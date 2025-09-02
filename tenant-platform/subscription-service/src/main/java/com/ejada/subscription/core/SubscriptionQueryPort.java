package com.ejada.subscription.core;

import java.time.Instant;
import java.util.UUID;

public interface SubscriptionQueryPort {
    ActiveSubscription loadActive(UUID tenantId);
    record ActiveSubscription(UUID subscriptionId, String tierId, Instant start, Instant end) {}
}
