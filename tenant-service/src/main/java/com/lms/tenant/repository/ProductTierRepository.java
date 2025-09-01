package com.lms.tenant.repository;

import com.lms.tenant.entity.ProductTier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProductTierRepository extends JpaRepository<ProductTier, UUID> {
}
