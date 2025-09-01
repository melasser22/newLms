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
    public PolicyResult consumeOrOverage(UUID tenantId, String feature, long totalUsage) {
        var sub = subscriptionQuery.loadActive(tenantId);
        var eff = featurePolicy.effective(sub.tierId(), tenantId, feature);

        if (!eff.enabled()) {
            return new PolicyResult(Long.MAX_VALUE, null);
        }

        long limit = eff.limit() == null ? Long.MAX_VALUE : eff.limit();
        if (totalUsage <= limit) {
            return new PolicyResult(limit, null);
        }

        if (!tenantSettings.isOverageEnabled(tenantId) || !eff.allowOverage()) {
            return new PolicyResult(limit, null);
        }

        long exceeded = totalUsage - limit;
        long price = eff.overageUnitPriceMinor() == null ? 0 : eff.overageUnitPriceMinor();
        var overageId = overagePort.recordOverage(tenantId, sub.subscriptionId(), feature, exceeded, price,
                eff.overageCurrency(), Instant.now(), Instant.now(), null);
        return new PolicyResult(limit, overageId);
    }

    public record PolicyResult(long limit, UUID overageId) {}
}
