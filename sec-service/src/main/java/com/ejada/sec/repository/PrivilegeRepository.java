package com.ejada.sec.repository;

import com.ejada.data.repository.TenantAwareRepository;
import com.ejada.sec.domain.Privilege;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PrivilegeRepository extends TenantAwareRepository<Privilege, Long> {

    Optional<Privilege> findByIdAndTenantId(Long id, UUID tenantId);

    Optional<Privilege> findByTenantIdAndCode(UUID tenantId, String code);

    List<Privilege> findAllByTenantId(UUID tenantId);

    List<Privilege> findAllByTenantIdAndResourceAndAction(UUID tenantId, String resource, String action);

    void deleteByIdAndTenantId(Long id, UUID tenantId);
}
