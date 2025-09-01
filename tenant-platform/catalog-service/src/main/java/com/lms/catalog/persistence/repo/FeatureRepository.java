package com.lms.catalog.persistence.repo;

import com.lms.catalog.persistence.entity.FeatureEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeatureRepository extends JpaRepository<FeatureEntity, String> {
}
