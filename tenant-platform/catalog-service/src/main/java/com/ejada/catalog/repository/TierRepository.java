package com.ejada.catalog.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import com.ejada.catalog.model.Tier;

import java.util.List;
import java.util.Optional;

@Repository
public interface TierRepository extends JpaRepository<Tier, Integer>, JpaSpecificationExecutor<Tier> {

    Optional<Tier> findByTierCd(String tierCd);

    boolean existsByTierCd(String tierCd);

    List<Tier> findByIsActiveTrueAndIsDeletedFalseOrderByRankOrderAsc();

    Page<Tier> findByIsDeletedFalse(Pageable pageable);

    Page<Tier> findByIsActiveAndIsDeletedFalse(boolean isActive, Pageable pageable);
}
