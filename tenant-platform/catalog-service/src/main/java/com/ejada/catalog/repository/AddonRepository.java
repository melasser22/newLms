package com.ejada.catalog.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import com.ejada.catalog.model.Addon;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddonRepository extends JpaRepository<Addon, Integer>, JpaSpecificationExecutor<Addon> {

    Optional<Addon> findByAddonCd(String addonCd);

    boolean existsByAddonCd(String addonCd);

    List<Addon> findByIsActiveTrueAndIsDeletedFalse();

    Page<Addon> findByIsDeletedFalse(Pageable pageable);

    Page<Addon> findByCategoryAndIsDeletedFalse(String category, Pageable pageable);
}
