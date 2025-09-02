package com.ejada.billing.infrastructure.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ejada.billing.domain.entities.TenantOverage;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link TenantOverage}.
 */
public interface TenantOverageRepository extends JpaRepository<TenantOverage, UUID> {

    Optional<TenantOverage> findByTenantIdAndIdempotencyKey(UUID tenantId, String idempotencyKey);
}

