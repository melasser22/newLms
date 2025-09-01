package com.lms.tenant.persistence.repository;

import com.lms.tenant.persistence.entity.TenantIntegrationKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantIntegrationKeyJpaRepository extends JpaRepository<TenantIntegrationKeyEntity, UUID> {

    /**
     * Finds all integration keys associated with a specific tenant.
     *
     * @param tenantId The UUID of the tenant.
     * @return A list of integration keys.
     */
    List<TenantIntegrationKeyEntity> findByTenantId(UUID tenantId);

    /**
     * Finds an integration key by its tenant and unique key prefix.
     *
     * @param tenantId The UUID of the tenant.
     * @param keyPrefix The key prefix.
     * @return An Optional containing the integration key if found.
     */
    Optional<TenantIntegrationKeyEntity> findByTenantIdAndKeyPrefix(UUID tenantId, String keyPrefix);
}
