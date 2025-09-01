package com.lms.tenant.repository;

import com.lms.tenant.entity.Feature;
import com.lms.tenant.entity.ProductTier;
import com.lms.tenant.entity.TierFeatureLimit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TierFeatureLimitRepository extends JpaRepository<TierFeatureLimit, UUID> {
    Optional<TierFeatureLimit> findByTierAndFeature(ProductTier tier, Feature feature);
}
