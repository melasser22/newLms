package com.lms.tenant.persistence.repo;

import com.lms.tenant.persistence.entity.TenantIntegrationKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TenantIntegrationKeyRepository extends JpaRepository<TenantIntegrationKey, UUID> { }
