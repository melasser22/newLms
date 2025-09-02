package com.ejada.catalog.repository;

import com.ejada.catalog.entity.TierFeatureLimitEntity;
import com.ejada.catalog.entity.TierFeatureLimitId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TierFeatureLimitRepository extends JpaRepository<TierFeatureLimitEntity, TierFeatureLimitId> {
    Optional<TierFeatureLimitEntity> findByIdTierIdAndIdFeatureKey(String tierId, String featureKey);
}
