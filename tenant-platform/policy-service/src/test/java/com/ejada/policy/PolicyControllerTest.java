package com.ejada.policy;

import com.ejada.tenant.core.FeaturePolicyPort;
import com.ejada.tenant.core.OveragePort;
import com.ejada.tenant.core.SubscriptionPort;
import com.ejada.tenant.core.TenantSettingsPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PolicyControllerTest {

    @Mock
    TenantSettingsPort tenantSettings;
    @Mock
    SubscriptionPort subscriptionPort;
    @Mock
    FeaturePolicyPort featurePolicy;
    @Mock
    OveragePort overagePort;
    @Mock
    UsageReader usageReader;

    @Test
    void recordsOverageWhenLimitExceeded() {
        var service = new PolicyService(tenantSettings, subscriptionPort, featurePolicy, overagePort);
        var controller = new PolicyController(service, usageReader);

        UUID tenantId = UUID.randomUUID();
        when(usageReader.currentUsage(eq(tenantId), eq("emails"), any(), any())).thenReturn(95L);
        when(tenantSettings.isOverageEnabled(tenantId)).thenReturn(true);
        var sub = new SubscriptionPort.ActiveSubscription(UUID.randomUUID(), "basic", Instant.now(), Instant.now());
        when(subscriptionPort.activeSubscription(tenantId)).thenReturn(sub);
        var eff = new FeaturePolicyPort.EffectiveFeature(true, 100L, true, 1L, "USD");
        when(featurePolicy.effective("basic", tenantId, "emails")).thenReturn(eff);
        UUID overageId = UUID.randomUUID();
        when(overagePort.recordOverage(eq(tenantId), eq(sub.subscriptionId()), eq("emails"), eq(15L), eq(1L),
                eq("USD"), any(Instant.class), any(Instant.class), isNull()))
                .thenReturn(overageId);

        var request = new PolicyController.ConsumeRequest("emails", 20L, Instant.now().minusSeconds(60), Instant.now(), null);
        var response = controller.consume(tenantId, request);

        assertTrue(response.allowed());
        assertEquals("emails", response.featureKey());
        assertEquals(100L, response.limit());
        assertEquals(95L, response.usedBefore());
        assertEquals(20L, response.requestedDelta());
        assertTrue(response.overageRecorded());
        assertEquals(overageId, response.overageId());
    }
}
