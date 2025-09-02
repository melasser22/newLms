package com.ejada.tenant.core;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class PolicyServiceTest {

    @Test
    void withinLimit() {
        FeaturePolicyPort fp = (tierId, tenantId, featureKey) -> new FeaturePolicyPort.EffectiveFeature(true, 10L, false, null, "USD");
        SubscriptionPort sp = tenantId -> new SubscriptionPort.ActiveSubscription(UUID.randomUUID(), "tier", Instant.EPOCH, Instant.EPOCH.plusSeconds(100));
        TenantSettingsPort tp = new TenantSettingsPort() {
            @Override
            public boolean isOverageEnabled(UUID tenantId) {
                return true;
            }

            @Override
            public void setOverageEnabled(UUID tenantId, boolean enabled) {
                // no-op for tests
            }
        };
        OveragePort op = (tenantId1, subscriptionId, featureKey, quantity, unitPriceMinor, currency, periodStart, periodEnd, idempotencyKey) -> {
            fail("overage should not be recorded");
            return null;
        };
        PolicyService service = new PolicyService(fp, sp, tp, new OverageService(op));

        PolicyService.EnforcementResult res = service.consumeOrOverage(UUID.randomUUID(), "f", 5, () -> 3L, null);
        assertTrue(res.allowed());
        assertEquals(0, res.overageRecorded());
    }

    @Test
    void exceededWithOverage() {
        FeaturePolicyPort fp = (tierId, tenantId, featureKey) -> new FeaturePolicyPort.EffectiveFeature(true, 10L, true, 100L, "USD");
        SubscriptionPort sp = tenantId -> new SubscriptionPort.ActiveSubscription(UUID.randomUUID(), "tier", Instant.EPOCH, Instant.EPOCH.plusSeconds(100));
        TenantSettingsPort tp = new TenantSettingsPort() {
            @Override
            public boolean isOverageEnabled(UUID tenantId) {
                return true;
            }

            @Override
            public void setOverageEnabled(UUID tenantId, boolean enabled) {
                // no-op for tests
            }
        };
        AtomicReference<UUID> recorded = new AtomicReference<>();
        OveragePort op = (tenantId1, subscriptionId, featureKey, quantity, unitPriceMinor, currency, periodStart, periodEnd, idempotencyKey) -> {
            recorded.set(UUID.randomUUID());
            assertEquals(4, quantity);
            assertEquals(100L, unitPriceMinor);
            assertEquals("USD", currency);
            return recorded.get();
        };
        PolicyService service = new PolicyService(fp, sp, tp, new OverageService(op));

        PolicyService.EnforcementResult res = service.consumeOrOverage(UUID.randomUUID(), "f", 5, () -> 9L, "key");
        assertTrue(res.allowed());
        assertEquals(4, res.overageRecorded());
        assertEquals(recorded.get(), res.overageId());
    }

    @Test
    void exceededBlocked() {
        FeaturePolicyPort fp = (tierId, tenantId, featureKey) -> new FeaturePolicyPort.EffectiveFeature(true, 10L, true, 100L, "USD");
        SubscriptionPort sp = tenantId -> new SubscriptionPort.ActiveSubscription(UUID.randomUUID(), "tier", Instant.EPOCH, Instant.EPOCH.plusSeconds(100));
        TenantSettingsPort tp = new TenantSettingsPort() {
            @Override
            public boolean isOverageEnabled(UUID tenantId) {
                return false; // tenant does not allow overage
            }

            @Override
            public void setOverageEnabled(UUID tenantId, boolean enabled) {
                // no-op for tests
            }
        };
        OveragePort op = (tenantId1, subscriptionId, featureKey, quantity, unitPriceMinor, currency, periodStart, periodEnd, idempotencyKey) -> {
            fail("overage should not be recorded when disabled");
            return null;
        };
        PolicyService service = new PolicyService(fp, sp, tp, new OverageService(op));

        PolicyService.EnforcementResult res = service.consumeOrOverage(UUID.randomUUID(), "f", 5, () -> 9L, "key");
        assertFalse(res.allowed());
        assertEquals(0, res.overageRecorded());
        assertNull(res.overageId());
    }
}
