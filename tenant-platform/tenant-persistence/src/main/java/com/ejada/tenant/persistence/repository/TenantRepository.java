package com.ejada.tenant.persistence.repository;

import com.ejada.tenant.persistence.entity.Tenant;
import com.ejada.tenant.persistence.entity.enums.TenantStatus;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for {@link Tenant} entities.
 */
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    long countByStatus(TenantStatus status);
}

