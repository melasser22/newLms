package com.lms.catalog.persistence.repo;

import com.lms.catalog.persistence.entity.ProductTierEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductTierRepository extends JpaRepository<ProductTierEntity, String> {
}
