package com.ejada.catalog.repository;

import com.ejada.catalog.model.TenantFeatureEntitlement;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantFeatureEntitlementRepository extends JpaRepository<TenantFeatureEntitlement, Long> {
    Optional<TenantFeatureEntitlement> findByTenantCodeAndFeatureCode(String tenantCode, String featureCode);
    List<TenantFeatureEntitlement> findByTenantCode(String tenantCode);
}
