package com.ejada.tenant.persistence.repository;

import com.ejada.tenant.persistence.entity.TenantIntegrationKey;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for {@link TenantIntegrationKey} entities.
 */
public interface TenantIntegrationKeyRepository extends JpaRepository<TenantIntegrationKey, UUID> {
}

