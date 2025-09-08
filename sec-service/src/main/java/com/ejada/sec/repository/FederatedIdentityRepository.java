package com.ejada.sec.repository;

import com.ejada.sec.domain.FederatedIdentity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FederatedIdentityRepository extends JpaRepository<FederatedIdentity, Long> {

    Optional<FederatedIdentity> findByProviderAndProviderUserId(String provider, String providerUserId);

    Optional<FederatedIdentity> findByTenantIdAndProviderAndProviderUserId(UUID tenantId, String provider, String providerUserId);
}
