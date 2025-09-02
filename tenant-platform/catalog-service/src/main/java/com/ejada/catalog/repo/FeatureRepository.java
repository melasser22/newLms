package com.ejada.catalog.repo;

import com.ejada.catalog.entity.FeatureEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeatureRepository extends JpaRepository<FeatureEntity, String> {
}
