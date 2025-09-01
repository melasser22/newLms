package com.lms.entitlement.repository;

import com.lms.entitlement.entity.EntitlementCache;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EntitlementCacheRepository extends JpaRepository<EntitlementCache, UUID> {
}
