package com.lms.tenant.core.shared;

import com.lms.tenant.core.port.SubscriptionPort;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.UUID;

/** Adapter using shared-library SubscriptionQueryService. */
class SharedSubscriptionPort implements SubscriptionPort {

    private final Object service;

    SharedSubscriptionPort(Object service) {
        this.service = service;
    }

    @Override
    public ActiveSubscription loadActive(UUID tenantId) {
        try {
            Method m = service.getClass().getMethod("findActiveByTenantId", UUID.class);
            Object result = m.invoke(service, tenantId);
            Class<?> cls = result.getClass();
            UUID subscriptionId = (UUID) cls.getMethod("subscriptionId").invoke(result);
            String tierId = (String) cls.getMethod("tierId").invoke(result);
            Instant periodStart = (Instant) cls.getMethod("periodStart").invoke(result);
            Instant periodEnd = (Instant) cls.getMethod("periodEnd").invoke(result);
            return new ActiveSubscription(subscriptionId, tierId, periodStart, periodEnd);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load active subscription", e);
        }
    }
}
