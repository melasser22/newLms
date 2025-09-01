package com.lms.tenant.core.service;

import com.lms.tenant.core.dto.EnforcementResult;
import com.lms.tenant.core.dto.RecordOverageRequest;
import com.lms.tenant.core.port.FeaturePolicyPort;
import com.lms.tenant.core.port.SubscriptionPort;
import com.lms.tenant.core.port.TenantSettingsPort;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.lang.Nullable;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/** Domain service handling feature policy enforcement and overage recording. */
public class PolicyService {

    private final SubscriptionPort subscriptionPort;
    private final FeaturePolicyPort featurePolicyPort;
    private final TenantSettingsPort tenantSettingsPort;
    private final OverageService overageService;

    public PolicyService(SubscriptionPort subscriptionPort,
                         FeaturePolicyPort featurePolicyPort,
                         TenantSettingsPort tenantSettingsPort,
                         OverageService overageService) {
        this.subscriptionPort = subscriptionPort;
        this.featurePolicyPort = featurePolicyPort;
        this.tenantSettingsPort = tenantSettingsPort;
        this.overageService = overageService;
    }

    @Transactional
    public EnforcementResult consumeOrOverage(UUID tenantId,
                                              String featureKey,
                                              long delta,
                                              Supplier<Long> currentUsageSupplier,
                                              @Nullable Instant periodStart,
                                              @Nullable Instant periodEnd,
                                              String idempotencyKey) {
        if (delta <= 0) {
            throw new IllegalArgumentException("delta must be positive");
        }

        var subscription = subscriptionPort.loadActive(tenantId);
        var feature = resolveFeature(subscription.tierId(), tenantId, featureKey);
        if (!feature.enabled()) {
            throw new IllegalStateException("Feature disabled: " + featureKey);
        }

        long used = currentUsageSupplier.get();
        Long limit = feature.limit();
        if (limit == null) {
            return EnforcementResult.allowedUnlimited(featureKey);
        }

        long newTotal = used + delta;
        if (newTotal <= limit) {
            return EnforcementResult.allowedWithinLimit(featureKey, limit, used, delta);
        }

        boolean tenantOverageEnabled = tenantSettingsPort.isOverageEnabled(tenantId);
        if (!tenantOverageEnabled || !feature.allowOverage()) {
            throw new IllegalStateException("Limit exceeded and overage disabled for " + featureKey);
        }

        long chargeable = Math.min(delta, newTotal - limit);
        var request = new RecordOverageRequest(featureKey,
                chargeable,
                feature.overageUnitPriceMinor(),
                feature.overageCurrency() != null ? feature.overageCurrency() : "USD",
                periodStart != null ? periodStart : subscription.periodStart(),
                periodEnd != null ? periodEnd : subscription.periodEnd(),
                idempotencyKey,
                Map.of());
        var response = overageService.record(tenantId, subscription.subscriptionId(), request);
        return EnforcementResult.allowedWithOverage(featureKey, limit, used, delta, chargeable, response.overageId());
    }

    @Cacheable(value = "entitlements_cache", key = "#tenantId + ':' + #featureKey")
    protected FeaturePolicyPort.EffectiveFeature resolveFeature(String tierId, UUID tenantId, String featureKey) {
        return featurePolicyPort.effective(tierId, tenantId, featureKey);
    }
}
