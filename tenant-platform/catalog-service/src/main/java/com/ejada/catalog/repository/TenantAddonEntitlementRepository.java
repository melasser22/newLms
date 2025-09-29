package com.ejada.catalog.repository;

import com.ejada.catalog.model.TenantAddonEntitlement;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantAddonEntitlementRepository extends JpaRepository<TenantAddonEntitlement, Long> {
    Optional<TenantAddonEntitlement> findByTenantCodeAndAddonCode(String tenantCode, String addonCode);
    List<TenantAddonEntitlement> findByTenantCode(String tenantCode);
}
