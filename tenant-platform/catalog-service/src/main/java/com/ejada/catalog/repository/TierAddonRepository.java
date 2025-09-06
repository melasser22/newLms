package com.ejada.catalog.repository;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.ejada.catalog.model.TierAddon;

import java.util.List;
import java.util.Optional;

@Repository
public interface TierAddonRepository extends JpaRepository<TierAddon, Integer>, JpaSpecificationExecutor<TierAddon> {

    Optional<TierAddon> findByTier_TierIdAndAddon_AddonId(Integer tierId, Integer addonId);

    boolean existsByTier_TierIdAndAddon_AddonId(Integer tierId, Integer addonId);

    List<TierAddon> findByTier_TierIdAndIsDeletedFalseOrderByIncludedDescSortOrderAsc(Integer tierId);

    Page<TierAddon> findByTier_TierIdAndIsDeletedFalse(Integer tierId, Pageable pageable);
}
