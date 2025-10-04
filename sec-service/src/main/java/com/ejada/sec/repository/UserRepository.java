package com.ejada.sec.repository;

import com.ejada.data.repository.TenantAwareRepository;
import com.ejada.sec.domain.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends TenantAwareRepository<User, Long> {

    Optional<User> findByIdAndTenantId(Long id, UUID tenantId);

    Optional<User> findByTenantIdAndUsername(UUID tenantId, String username);

    Optional<User> findByTenantIdAndEmail(UUID tenantId, String email);

    boolean existsByTenantIdAndUsername(UUID tenantId, String username);

    boolean existsByTenantIdAndEmail(UUID tenantId, String email);

    List<User> findAllByTenantId(UUID tenantId);

    void deleteByIdAndTenantId(Long id, UUID tenantId);
}
