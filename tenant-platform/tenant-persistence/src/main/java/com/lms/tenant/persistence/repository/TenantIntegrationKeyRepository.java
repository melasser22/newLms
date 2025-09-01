package com.lms.tenant.persistence.repository;

import com.lms.tenant.persistence.entity.TenantIntegrationKey;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for {@link TenantIntegrationKey} entities.
 */
public interface TenantIntegrationKeyRepository extends JpaRepository<TenantIntegrationKey, UUID> {
}

