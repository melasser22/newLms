package com.ejada.email.usage.repository;

import com.ejada.email.usage.domain.TenantQuota;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantQuotaRepository extends JpaRepository<TenantQuota, Long> {
  Optional<TenantQuota> findByTenantId(String tenantId);
}
