package com.lms.entitlement.repository;

import com.lms.entitlement.entity.Feature;
import com.lms.entitlement.entity.Tenant;
import com.lms.entitlement.entity.TenantFeatureOverride;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TenantFeatureOverrideRepository extends JpaRepository<TenantFeatureOverride, UUID> {
    Optional<TenantFeatureOverride> findByTenantAndFeature(Tenant tenant, Feature feature);
}
