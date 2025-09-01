package com.lms.tenant.persistence.repo;

import com.lms.tenant.persistence.entity.Tenant;
import com.lms.tenant.persistence.entity.TenantStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {
  Optional<Tenant> findBySlug(String slug);
  long countByStatus(TenantStatus status);
}
