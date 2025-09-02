package com.ejada.billing.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ejada.billing.entity.TenantOverage;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link TenantOverage}.
 */
public interface TenantOverageRepository extends JpaRepository<TenantOverage, UUID> {

    Optional<TenantOverage> findByTenantIdAndIdempotencyKey(UUID tenantId, String idempotencyKey);
}

