package com.lms.tenant.repository;

import com.lms.tenant.entity.SubscriptionStatus;
import com.lms.tenant.entity.Tenant;
import com.lms.tenant.entity.TenantSubscription;
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
