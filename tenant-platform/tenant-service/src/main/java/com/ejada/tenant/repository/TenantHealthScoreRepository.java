package com.ejada.tenant.repository;

import com.ejada.tenant.model.TenantHealthScore;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantHealthScoreRepository extends JpaRepository<TenantHealthScore, Long> {

    Optional<TenantHealthScore> findFirstByTenant_IdOrderByEvaluatedAtDesc(Integer tenantId);
}
