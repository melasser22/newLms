package com.lms.tenant.repository;

import com.lms.tenant.entity.TenantOverage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TenantOverageRepository extends JpaRepository<TenantOverage, UUID> {
    Optional<TenantOverage> findByIdemKey(String idemKey);
}
