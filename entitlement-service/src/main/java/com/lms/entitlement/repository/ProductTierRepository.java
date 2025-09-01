package com.lms.entitlement.repository;

import com.lms.entitlement.entity.ProductTier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProductTierRepository extends JpaRepository<ProductTier, UUID> {
}
