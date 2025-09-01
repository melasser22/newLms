package com.lms.tenant.core.shared;

import com.lms.tenant.core.port.OveragePort;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Adapter using shared-library OverageService if present. */
class SharedOveragePort implements OveragePort {

    private final Object overageService;

    SharedOveragePort(Object overageService) {
        this.overageService = overageService;
    }

    @Override
    public UUID recordOverage(UUID tenantId, UUID subscriptionId, String featureKey, long quantity, Long unitPriceMinor,
                              String currency, Instant periodStart, Instant periodEnd, String idempotencyKey,
                              Map<String, Object> metadata) {
        try {
            Class<?> dtoClass = Class.forName("com.shared.billing.api.OverageRecordDTO");
            Constructor<?> ctor = dtoClass.getConstructor(UUID.class, UUID.class, String.class, long.class, Long.class,
                    String.class, Instant.class, Instant.class, String.class, Map.class);
            Object dto = ctor.newInstance(tenantId, subscriptionId, featureKey, quantity, unitPriceMinor, currency,
                    periodStart, periodEnd, idempotencyKey, metadata);
            Method m = overageService.getClass().getMethod("createOverage", dtoClass);
            Object result = m.invoke(overageService, dto);
            return (UUID) result;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to invoke shared overage service", e);
        }
    }
}
