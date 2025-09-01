package com.lms.tenant.repository;

import com.lms.tenant.entity.EntitlementCache;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EntitlementCacheRepository extends JpaRepository<EntitlementCache, UUID> {
}
