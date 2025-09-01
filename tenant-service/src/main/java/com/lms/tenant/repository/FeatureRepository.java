package com.lms.tenant.repository;

import com.lms.tenant.entity.Feature;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeatureRepository extends JpaRepository<Feature, String> {
}
