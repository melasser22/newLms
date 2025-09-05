package com.ejada.catalog.repository;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.ejada.catalog.model.AddonFeature;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddonFeatureRepository extends JpaRepository<AddonFeature, Integer>, JpaSpecificationExecutor<AddonFeature> {

    Optional<AddonFeature> findByAddon_AddonIdAndFeature_FeatureId(Integer addonId, Integer featureId);

    boolean existsByAddon_AddonIdAndFeature_FeatureId(Integer addonId, Integer featureId);

    List<AddonFeature> findByAddon_AddonIdAndEnabledTrueAndIsDeletedFalse(Integer addonId);

    List<AddonFeature> findByAddon_AddonIdAndIsDeletedFalse(Integer addonId);

    Page<AddonFeature> findByAddon_AddonIdAndIsDeletedFalse(Integer addonId, Pageable pageable);
}
