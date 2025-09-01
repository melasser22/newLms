package com.lms.entitlement.repository;

import com.lms.entitlement.entity.TenantOverage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TenantOverageRepository extends JpaRepository<TenantOverage, UUID> {
    Optional<TenantOverage> findByIdemKey(String idemKey);
}
