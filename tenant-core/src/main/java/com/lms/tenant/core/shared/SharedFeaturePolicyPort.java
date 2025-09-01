package com.lms.tenant.core.shared;

import com.lms.tenant.core.port.FeaturePolicyPort;

import java.lang.reflect.Method;
import java.util.UUID;

/** Adapter using shared-library FeaturePolicyService. */
class SharedFeaturePolicyPort implements FeaturePolicyPort {

    private final Object service;

    SharedFeaturePolicyPort(Object service) {
        this.service = service;
    }

    @Override
    public EffectiveFeature effective(String tierId, UUID tenantId, String featureKey) {
        try {
            Method m = service.getClass().getMethod("resolveEffectiveFeature", String.class, UUID.class, String.class);
            Object result = m.invoke(service, tierId, tenantId, featureKey);
            Class<?> cls = result.getClass();
            boolean enabled = (boolean) cls.getMethod("enabled").invoke(result);
            Object limitObj = cls.getMethod("limit").invoke(result);
            Long limit = limitObj == null ? null : (Long) limitObj;
            boolean allowOverage = (boolean) cls.getMethod("allowOverage").invoke(result);
            Object priceObj = cls.getMethod("overageUnitPriceMinor").invoke(result);
            Long price = priceObj == null ? null : (Long) priceObj;
            String currency = (String) cls.getMethod("overageCurrency").invoke(result);
            return new EffectiveFeature(enabled, limit, allowOverage, price, currency);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to resolve feature policy", e);
        }
    }
}
