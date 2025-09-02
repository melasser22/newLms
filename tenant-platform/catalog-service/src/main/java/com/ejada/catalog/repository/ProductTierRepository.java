package com.ejada.catalog.repository;

import com.ejada.catalog.entity.ProductTierEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductTierRepository extends JpaRepository<ProductTierEntity, String> {
}
