package com.ejada.tenant.subscription.repo;

import com.ejada.tenant.subscription.domain.TenantSubscription;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TenantSubscriptionRepository extends JpaRepository<TenantSubscription, UUID> {

    @Query("select s from TenantSubscription s where s.tenantId = :tenantId and s.active = true")
    Optional<TenantSubscription> findActiveByTenantId(UUID tenantId);
}
