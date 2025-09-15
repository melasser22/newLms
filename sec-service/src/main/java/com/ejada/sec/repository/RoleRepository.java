package com.ejada.sec.repository;

import com.ejada.sec.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByTenantIdAndCode(UUID tenantId, String code);

    List<Role> findAllByTenantId(UUID tenantId);

    boolean existsByTenantIdAndCode(UUID tenantId, String code);
}
