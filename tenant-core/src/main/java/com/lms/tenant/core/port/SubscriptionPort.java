package com.lms.tenant.core.port;

import java.time.Instant;
import java.util.UUID;

/** Port to load the active subscription for a tenant. */
public interface SubscriptionPort {

    record ActiveSubscription(UUID subscriptionId, String tierId, Instant periodStart, Instant periodEnd) {}

    ActiveSubscription loadActive(UUID tenantId);
}
