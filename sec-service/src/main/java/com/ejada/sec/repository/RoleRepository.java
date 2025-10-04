package com.ejada.sec.repository;

import com.ejada.data.repository.TenantAwareRepository;
import com.ejada.sec.domain.Role;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends TenantAwareRepository<Role, Long> {

    Optional<Role> findByIdAndTenantId(Long id, UUID tenantId);

    Optional<Role> findByTenantIdAndCode(UUID tenantId, String code);

    List<Role> findAllByTenantId(UUID tenantId);

    boolean existsByTenantIdAndCode(UUID tenantId, String code);

    void deleteByIdAndTenantId(Long id, UUID tenantId);
}
