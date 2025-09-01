package com.lms.entitlement.repository;

import com.lms.entitlement.entity.SubscriptionStatus;
import com.lms.entitlement.entity.Tenant;
import com.lms.entitlement.entity.TenantSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface TenantSubscriptionRepository extends JpaRepository<TenantSubscription, UUID> {
    Optional<TenantSubscription> findFirstByTenantAndStatusInAndPeriodStartLessThanEqualAndPeriodEndGreaterThan(
            Tenant tenant,
            Set<SubscriptionStatus> statuses,
            Instant periodStart,
            Instant periodEnd);
}
