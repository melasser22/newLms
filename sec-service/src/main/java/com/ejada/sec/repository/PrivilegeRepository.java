package com.ejada.sec.repository;

import com.ejada.sec.domain.Privilege;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PrivilegeRepository extends JpaRepository<Privilege, Long> {

    Optional<Privilege> findByTenantIdAndCode(UUID tenantId, String code);

    List<Privilege> findAllByTenantId(UUID tenantId);

    List<Privilege> findAllByTenantIdAndResourceAndAction(UUID tenantId, String resource, String action);
}
