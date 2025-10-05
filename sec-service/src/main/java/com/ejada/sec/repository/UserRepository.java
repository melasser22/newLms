package com.ejada.sec.repository;

import com.ejada.common.context.ContextManager;
import com.ejada.sec.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByTenantIdAndUsername(UUID tenantId, String username);

    Optional<User> findByTenantIdAndEmail(UUID tenantId, String email);

    Optional<User> findByTenantIdAndId(UUID tenantId, Long id);

    boolean existsByTenantIdAndUsername(UUID tenantId, String username);

    boolean existsByTenantIdAndEmail(UUID tenantId, String email);

    boolean existsByTenantIdAndId(UUID tenantId, Long id);

    long countByTenantId(UUID tenantId);

    void deleteByTenantIdAndId(UUID tenantId, Long id);

    List<User> findAllByTenantId(UUID tenantId);

    default List<User> findAllSecure() {
        return findAllByTenantId(requireTenant());
    }

    default Optional<User> findByIdSecure(Long id) {
        return findByTenantIdAndId(requireTenant(), id);
    }

    default boolean existsByIdSecure(Long id) {
        return existsByTenantIdAndId(requireTenant(), id);
    }

    default long countSecure() {
        return countByTenantId(requireTenant());
    }

    @Transactional
    default void deleteByIdSecure(Long id) {
        deleteByTenantIdAndId(requireTenant(), id);
    }

    private UUID requireTenant() {
        String tenant = ContextManager.Tenant.get();
        if (tenant == null || tenant.isBlank()) {
            throw new IllegalStateException("Tenant context is not available");
        }
        try {
            return UUID.fromString(tenant);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("Tenant context is invalid", ex);
        }
    }
}
