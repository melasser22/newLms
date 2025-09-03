package com.ejada.billing.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ejada.billing.entity.TenantOverage;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link TenantOverage}.
 */
@Repository
public interface TenantOverageRepository extends JpaRepository<TenantOverage, UUID> {

    Optional<TenantOverage> findByTenantIdAndIdempotencyKey(UUID tenantId, String idempotencyKey);
}

