package com.lms.policy;

import com.lms.billing.core.OveragePort;
import com.lms.catalog.core.FeaturePolicyPort;
import com.lms.subscription.core.SubscriptionQueryPort;
import com.lms.tenant.core.TenantSettingsPort;
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
    SubscriptionQueryPort subscriptionQuery;
    @Mock
    FeaturePolicyPort featurePolicy;
    @Mock
    OveragePort overagePort;
    @Mock
    UsageReader usageReader;

    @Test
    void recordsOverageWhenLimitExceeded() {
        var service = new PolicyService(tenantSettings, subscriptionQuery, featurePolicy, overagePort);
        var controller = new PolicyController(service, usageReader);

        UUID tenantId = UUID.randomUUID();
        when(usageReader.currentUsage(eq(tenantId), eq("emails"), any(), any())).thenReturn(95L);
        when(tenantSettings.isOverageEnabled(tenantId)).thenReturn(true);
        var sub = new SubscriptionQueryPort.ActiveSubscription(UUID.randomUUID(), "basic", Instant.now(), Instant.now());
        when(subscriptionQuery.loadActive(tenantId)).thenReturn(sub);
        var eff = new FeaturePolicyPort.EffectiveFeature(true, 100L, true, 1L, "USD");
        when(featurePolicy.effective("basic", tenantId, "emails")).thenReturn(eff);
        UUID overageId = UUID.randomUUID();
        when(overagePort.recordOverage(eq(tenantId), eq(sub.subscriptionId()), eq("emails"), eq(15L), any(), eq("USD"), any(), any(), isNull()))
                .thenReturn(overageId);

        var request = new PolicyController.ConsumeRequest("emails", 20L, null, null, null);
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
