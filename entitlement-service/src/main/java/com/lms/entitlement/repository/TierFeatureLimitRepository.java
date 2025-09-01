package com.lms.entitlement.repository;

import com.lms.entitlement.entity.Feature;
import com.lms.entitlement.entity.ProductTier;
import com.lms.entitlement.entity.TierFeatureLimit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TierFeatureLimitRepository extends JpaRepository<TierFeatureLimit, UUID> {
    Optional<TierFeatureLimit> findByTierAndFeature(ProductTier tier, Feature feature);
}
