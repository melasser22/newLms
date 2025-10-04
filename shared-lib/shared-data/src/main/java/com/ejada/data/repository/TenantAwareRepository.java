package com.ejada.data.repository;

import com.ejada.starter_core.context.TenantContextResolver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Base repository enforcing tenant isolation.
 * All queries automatically filter by current tenant context.
 */
@NoRepositoryBean
public interface TenantAwareRepository<T, ID> extends JpaRepository<T, ID> {

    // Find by ID with tenant check
    default Optional<T> findByIdSecure(ID id) {
        UUID tenantId = TenantContextResolver.requireTenantId();
        return findByIdAndTenantId(id, tenantId);
    }

    Optional<T> findByIdAndTenantId(ID id, UUID tenantId);

    // List all for current tenant
    default List<T> findAllSecure() {
        UUID tenantId = TenantContextResolver.requireTenantId();
        return findAllByTenantId(tenantId);
    }

    List<T> findAllByTenantId(UUID tenantId);

    // Delete with tenant check
    default void deleteSecure(ID id) {
        UUID tenantId = TenantContextResolver.requireTenantId();
        deleteByIdAndTenantId(id, tenantId);
    }

    void deleteByIdAndTenantId(ID id, UUID tenantId);
}
