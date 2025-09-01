package com.lms.tenant.repository;

import com.lms.tenant.entity.Feature;
import com.lms.tenant.entity.Tenant;
import com.lms.tenant.entity.TenantFeatureOverride;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TenantFeatureOverrideRepository extends JpaRepository<TenantFeatureOverride, UUID> {
    Optional<TenantFeatureOverride> findByTenantAndFeature(Tenant tenant, Feature feature);
}
