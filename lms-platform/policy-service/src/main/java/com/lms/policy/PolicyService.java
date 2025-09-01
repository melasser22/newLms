package com.lms.policy;

import com.lms.billing.core.OveragePort;
import com.lms.catalog.core.FeaturePolicyPort;
import com.lms.subscription.core.SubscriptionQueryPort;
import com.lms.tenant.core.TenantSettingsPort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class PolicyService {

    private final TenantSettingsPort tenantSettings;
    private final SubscriptionQueryPort subscriptionQuery;
    private final FeaturePolicyPort featurePolicy;
    private final OveragePort overagePort;

    public PolicyService(TenantSettingsPort tenantSettings,
                         SubscriptionQueryPort subscriptionQuery,
                         FeaturePolicyPort featurePolicy,
                         OveragePort overagePort) {
        this.tenantSettings = tenantSettings;
        this.subscriptionQuery = subscriptionQuery;
        this.featurePolicy = featurePolicy;
        this.overagePort = overagePort;
    }

    public UUID consumeOrOverage(UUID tenantId, String tierId, String feature, long delta) {
        var sub = subscriptionQuery.loadActive(tenantId);
        var eff = featurePolicy.effective(tierId, tenantId, feature);

        if (!eff.enabled()) return null;

        long limit = eff.limit() == null ? Long.MAX_VALUE : eff.limit();
        if (delta <= limit) return null;

        if (!tenantSettings.isOverageEnabled(tenantId) || !eff.allowOverage()) {
            return null;
        }

        long exceeded = delta - limit;
        long price = eff.overageUnitPriceMinor() == null ? 0 : eff.overageUnitPriceMinor();
        return overagePort.recordOverage(tenantId, sub.subscriptionId(), feature, exceeded, price,
                eff.overageCurrency(), Instant.now(), Instant.now(), null);
    }
}
