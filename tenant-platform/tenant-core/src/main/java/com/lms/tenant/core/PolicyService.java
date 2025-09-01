package com.lms.tenant.core;

import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class PolicyService {
    private final FeaturePolicyPort featurePolicyPort;
    private final SubscriptionPort subscriptionPort;
    private final TenantSettingsPort tenantSettingsPort;
    private final OverageService overageService;

    public PolicyService(FeaturePolicyPort featurePolicyPort,
                         SubscriptionPort subscriptionPort,
                         TenantSettingsPort tenantSettingsPort,
                         OverageService overageService) {
        this.featurePolicyPort = featurePolicyPort;
        this.subscriptionPort = subscriptionPort;
        this.tenantSettingsPort = tenantSettingsPort;
        this.overageService = overageService;
    }

    public EnforcementResult consumeOrOverage(UUID tenantId,
                                               String featureKey,
                                               long delta,
                                               Supplier<Long> currentUsageSupplier,
                                               String idempotencyKey) {
        SubscriptionPort.ActiveSubscription sub = subscriptionPort.activeSubscription(tenantId);
        FeaturePolicyPort.EffectiveFeature feature =
                featurePolicyPort.effective(sub.tierId(), tenantId, featureKey);

        long used = Optional.ofNullable(currentUsageSupplier.get()).orElse(0L);
        Long limit = feature.limit();
        if (!feature.enabled()) {
            return EnforcementResult.blocked(featureKey, limit, used, delta);
        }
        if (limit == null || used + delta <= limit) {
            return EnforcementResult.allowedWithinLimit(featureKey, limit, used, delta);
        }
        long over = used + delta - limit;
        if (feature.allowOverage() && tenantSettingsPort.isOverageEnabled(tenantId)) {
            UUID overageId = overageService.record(tenantId, sub.subscriptionId(), featureKey,
                    over, feature.overageUnitPriceMinor(), feature.overageCurrency(),
                    sub.periodStart(), sub.periodEnd(), idempotencyKey);
            return EnforcementResult.allowedWithOverage(featureKey, limit, used, delta, over, overageId);
        }
        return EnforcementResult.blocked(featureKey, limit, used, delta);
    }

    public record EnforcementResult(boolean allowed,
                                    String featureKey,
                                    Long limit,
                                    long usedBefore,
                                    long requestedDelta,
                                    long overageRecorded,
                                    UUID overageId) {
        public static EnforcementResult allowedWithinLimit(String f, Long l, long u, long d) {
            return new EnforcementResult(true, f, l, u, d, 0, null);
        }

        public static EnforcementResult allowedWithOverage(String f, Long l, long u, long d, long overQty, UUID id) {
            return new EnforcementResult(true, f, l, u, d, overQty, id);
        }

        public static EnforcementResult blocked(String f, Long l, long u, long d) {
            return new EnforcementResult(false, f, l, u, d, 0, null);
        }
    }
}
