package com.lms.tenant.service;

import com.lms.tenant.entity.*;
import com.lms.tenant.repository.*;
import com.lms.tenant.service.dto.OverageRecordDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class PolicyService {

    private static final Set<SubscriptionStatus> ACTIVE_STATUSES =
            EnumSet.of(SubscriptionStatus.TRIALING, SubscriptionStatus.ACTIVE, SubscriptionStatus.PAST_DUE);

    private final TenantRepository tenantRepository;
    private final FeatureRepository featureRepository;
    private final TenantSubscriptionRepository subscriptionRepository;
    private final TierFeatureLimitRepository tierFeatureLimitRepository;
    private final TenantFeatureOverrideRepository tenantFeatureOverrideRepository;
    private final OverageService overageService;

    public PolicyService(TenantRepository tenantRepository,
                         FeatureRepository featureRepository,
                         TenantSubscriptionRepository subscriptionRepository,
                         TierFeatureLimitRepository tierFeatureLimitRepository,
                         TenantFeatureOverrideRepository tenantFeatureOverrideRepository,
                         OverageService overageService) {
        this.tenantRepository = tenantRepository;
        this.featureRepository = featureRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.tierFeatureLimitRepository = tierFeatureLimitRepository;
        this.tenantFeatureOverrideRepository = tenantFeatureOverrideRepository;
        this.overageService = overageService;
    }

    @Transactional
    public Optional<OverageRecordDto> consumeOrOverage(UUID tenantId, String featureKey, long delta,
                                                       Instant periodStart, Instant periodEnd,
                                                       Supplier<Long> currentUsageSupplier,
                                                       String idemKey) {
        Tenant tenant = tenantRepository.findById(tenantId).orElseThrow();
        Feature feature = featureRepository.findById(featureKey).orElseThrow();
        TenantSubscription subscription = subscriptionRepository
                .findFirstByTenantAndStatusInAndPeriodStartLessThanEqualAndPeriodEndGreaterThan(
                        tenant, ACTIVE_STATUSES, periodStart, periodEnd)
                .orElseThrow();

        // base limit from tier
        TierFeatureLimit tierLimit = tierFeatureLimitRepository
                .findByTierAndFeature(subscription.getTier(), feature)
                .orElseThrow();
        Long limit = tierLimit.getFeatureLimit();
        Boolean allowOverage = tierLimit.getAllowOverage();
        Long priceMinor = tierLimit.getOverageUnitPriceMinor();
        String currency = tierLimit.getOverageCurrency() != null ? tierLimit.getOverageCurrency() : "USD";

        // tenant override
        tenantFeatureOverrideRepository.findByTenantAndFeature(tenant, feature).ifPresent(ov -> {
            if (ov.getFeatureLimit() != null) limit = ov.getFeatureLimit();
            if (ov.getAllowOverage() != null) allowOverage = ov.getAllowOverage();
            if (ov.getOverageUnitPriceMinor() != null) priceMinor = ov.getOverageUnitPriceMinor();
            if (ov.getOverageCurrency() != null) currency = ov.getOverageCurrency();
        });

        if (limit == null) {
            // unlimited
            return Optional.empty();
        }
        long current = currentUsageSupplier.get();
        long newUsage = current + delta;
        if (newUsage <= limit) {
            return Optional.empty();
        }
        long exceeded = newUsage - limit;
        long chargeable = Math.min(delta, exceeded);

        if (!(Boolean.TRUE.equals(tenant.isOverageEnabled()) && Boolean.TRUE.equals(allowOverage))) {
            throw new IllegalStateException("Overage not allowed");
        }
        OverageRecordDto record = overageService.record(tenantId, featureKey, chargeable,
                priceMinor != null ? priceMinor : 0L, currency, periodStart, periodEnd, idemKey);
        return Optional.of(record);
    }
}
