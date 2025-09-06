package com.ejada.catalog.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import com.ejada.catalog.model.Feature;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeatureRepository extends JpaRepository<Feature, Integer>, JpaSpecificationExecutor<Feature> {

    Optional<Feature> findByFeatureKey(String featureKey);

    boolean existsByFeatureKey(String featureKey);

    List<Feature> findByIsActiveTrueAndIsDeletedFalse();

    Page<Feature> findByIsDeletedFalse(Pageable pageable);

    Page<Feature> findByCategoryAndIsDeletedFalse(String category, Pageable pageable);
}
