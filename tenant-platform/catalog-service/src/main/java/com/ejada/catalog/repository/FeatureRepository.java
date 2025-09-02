package com.ejada.catalog.repository;

import com.ejada.catalog.entity.FeatureEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeatureRepository extends JpaRepository<FeatureEntity, String> {
}
