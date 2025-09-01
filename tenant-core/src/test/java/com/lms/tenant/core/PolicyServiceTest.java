package com.lms.tenant.core;

import com.lms.tenant.core.dto.EnforcementResult;
import com.lms.tenant.core.port.FeaturePolicyPort;
import com.lms.tenant.core.port.OveragePort;
import com.lms.tenant.core.port.SubscriptionPort;
import com.lms.tenant.core.port.TenantSettingsPort;
import com.lms.tenant.core.service.OverageService;
import com.lms.tenant.core.service.PolicyService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class PolicyServiceTest {

    private final UUID tenantId = UUID.randomUUID();
    private final SubscriptionPort subscriptionPort = t ->
            new SubscriptionPort.ActiveSubscription(UUID.randomUUID(), "tier", Instant.EPOCH, Instant.EPOCH.plusSeconds(3600));

    @Test
    void withinLimitAllowed() {
        FeaturePolicyPort featurePort = (tier, tid, f) ->
                new FeaturePolicyPort.EffectiveFeature(true, 10L, true, null, "USD");
        TenantSettingsPort tenantSettings = new TenantSettingsPort() {
            @Override public boolean isOverageEnabled(UUID tenantId) { return true; }
            @Override public void setOverageEnabled(UUID tenantId, boolean enabled) { }
        };
        OveragePort overagePort = (tenantId, subscriptionId, featureKey, quantity, unitPriceMinor, currency, periodStart, periodEnd, idempotencyKey, metadata) -> UUID.randomUUID();
        PolicyService service = new PolicyService(subscriptionPort, featurePort, tenantSettings, new OverageService(overagePort));

        var result = service.consumeOrOverage(tenantId, "feature", 2, () -> 5L, null, null, "id1");
        assertEquals(EnforcementResult.Type.ALLOWED_WITHIN_LIMIT, result.type());
        assertEquals(0, result.overageRecorded());
    }

    @Test
    void exceedWithOverageEnabledRecordsOverage() {
        FeaturePolicyPort featurePort = (tier, tid, f) ->
                new FeaturePolicyPort.EffectiveFeature(true, 10L, true, 100L, "USD");
        TenantSettingsPort tenantSettings = new TenantSettingsPort() {
            @Override public boolean isOverageEnabled(UUID tenantId) { return true; }
            @Override public void setOverageEnabled(UUID tenantId, boolean enabled) { }
        };
        AtomicLong recorded = new AtomicLong();
        OveragePort overagePort = (tenantId, subscriptionId, featureKey, quantity, unitPriceMinor, currency, periodStart, periodEnd, idempotencyKey, metadata) -> {
            recorded.set(quantity);
            return UUID.randomUUID();
        };
        PolicyService service = new PolicyService(subscriptionPort, featurePort, tenantSettings, new OverageService(overagePort));

        var result = service.consumeOrOverage(tenantId, "feature", 5, () -> 9L, null, null, "id2");
        assertEquals(EnforcementResult.Type.ALLOWED_WITH_OVERAGE, result.type());
        assertEquals(4, result.overageRecorded());
        assertEquals(4, recorded.get());
    }

    @Test
    void exceedWithOverageDisabledThrows() {
        FeaturePolicyPort featurePort = (tier, tid, f) ->
                new FeaturePolicyPort.EffectiveFeature(true, 10L, true, 100L, "USD");
        TenantSettingsPort tenantSettings = new TenantSettingsPort() {
            @Override public boolean isOverageEnabled(UUID tenantId) { return false; }
            @Override public void setOverageEnabled(UUID tenantId, boolean enabled) { }
        };
        OveragePort overagePort = (tenantId, subscriptionId, featureKey, quantity, unitPriceMinor, currency, periodStart, periodEnd, idempotencyKey, metadata) -> UUID.randomUUID();
        PolicyService service = new PolicyService(subscriptionPort, featurePort, tenantSettings, new OverageService(overagePort));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.consumeOrOverage(tenantId, "feature", 5, () -> 9L, null, null, "id3"));
        assertTrue(ex.getMessage().contains("overage disabled"));
    }
}
