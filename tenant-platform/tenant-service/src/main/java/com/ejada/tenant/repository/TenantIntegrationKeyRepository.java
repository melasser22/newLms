package com.ejada.tenant.repository;

import com.ejada.tenant.model.TenantIntegrationKey;
import com.ejada.tenant.model.TenantIntegrationKey.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TenantIntegrationKeyRepository extends JpaRepository<TenantIntegrationKey, Long> {

    Optional<TenantIntegrationKey> findByTikIdAndIsDeletedFalse(Long tikId);

    Optional<TenantIntegrationKey> findByTenant_IdAndKeyIdAndIsDeletedFalse(Integer tenantId, String keyId);

    Page<TenantIntegrationKey> findByTenant_IdAndIsDeletedFalse(Integer tenantId, Pageable pageable);

    List<TenantIntegrationKey> findByTenant_IdAndStatusAndIsDeletedFalse(Integer tenantId, Status status);

    boolean existsByTenant_IdAndKeyIdAndIsDeletedFalse(Integer tenantId, String keyId);

    // --- Usable (active + within validity window + not deleted) ---
    @Query("""
           select k
             from TenantIntegrationKey k
            where k.tenant.id = :tenantId
              and k.keyId     = :keyId
              and k.isDeleted = false
              and k.status    = com.ejada.tenant.model.TenantIntegrationKey$Status.ACTIVE
              and k.validFrom <= CURRENT_TIMESTAMP
              and k.expiresAt >  CURRENT_TIMESTAMP
           """)
    Optional<TenantIntegrationKey> findUsableKey(@Param("tenantId") Integer tenantId,
                                                 @Param("keyId") String keyId);

    // Expired but not marked EXPIRED (useful for maintenance jobs)
    @Query("""
           select k
             from TenantIntegrationKey k
            where k.isDeleted = false
              and k.expiresAt <= CURRENT_TIMESTAMP
              and k.status <> com.ejada.tenant.model.TenantIntegrationKey$Status.EXPIRED
           """)
    List<TenantIntegrationKey> findAllExpiredAndNotMarked();

    // Optional soft-delete convenience (if you prefer doing it in repository)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
           update TenantIntegrationKey k
              set k.isDeleted = true,
                  k.status    = com.ejada.tenant.model.TenantIntegrationKey$Status.REVOKED
            where k.tikId     = :id
              and k.isDeleted = false
           """)
    int softDeleteById(@Param("id") Long id);
}