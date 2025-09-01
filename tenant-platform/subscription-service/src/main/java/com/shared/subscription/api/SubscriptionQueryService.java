package com.shared.subscription.api;

import java.util.Optional;
import java.util.UUID;

public interface SubscriptionQueryService {
    Optional<SubscriptionDto> findActiveSubscription(UUID tenantId);
}
