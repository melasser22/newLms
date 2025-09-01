package com.lms.billing.persistence.repo;

import com.lms.billing.persistence.entity.TenantOverage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link TenantOverage}.
 */
public interface TenantOverageRepository extends JpaRepository<TenantOverage, UUID> {

    Optional<TenantOverage> findByTenantIdAndIdempotencyKey(UUID tenantId, String idempotencyKey);
}

