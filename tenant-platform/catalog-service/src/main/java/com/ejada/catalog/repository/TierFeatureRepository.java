package com.ejada.catalog.repository;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.ejada.catalog.model.TierFeature;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TierFeatureRepository extends JpaRepository<TierFeature, Integer>, JpaSpecificationExecutor<TierFeature> {

    Optional<TierFeature> findByTier_TierIdAndFeature_FeatureId(Integer tierId, Integer featureId);

    boolean existsByTier_TierIdAndFeature_FeatureId(Integer tierId, Integer featureId);

    List<TierFeature> findByTier_TierIdAndIsDeletedFalse(Integer tierId);

    List<TierFeature> findByTier_TierIdAndEnabledTrueAndIsDeletedFalse(Integer tierId);

    Page<TierFeature> findByTier_TierIdAndIsDeletedFalse(Integer tierId, Pageable pageable);

    List<TierFeature> findByTier_TierIdInAndIsDeletedFalse(Collection<Integer> tierIds);
}
