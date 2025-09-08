package com.ejada.tenant.repository;

import com.ejada.tenant.model.TenantIntegrationKey;
import com.ejada.tenant.model.TenantIntegrationKey.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TenantIntegrationKeyRepository extends JpaRepository<TenantIntegrationKey, Long> {

    Optional<TenantIntegrationKey> findByTikIdAndIsDeletedFalse(Long tikId);

    @Query("""
           select k
             from TenantIntegrationKey k
            where k.tenant.id = :tenantId
              and k.keyId     = :keyId
              and k.isDeleted = false
           """)
    Optional<TenantIntegrationKey> findByTenantIdAndKeyIdAndIsDeletedFalse(@Param("tenantId") Integer tenantId,
                                                                           @Param("keyId") String keyId);

    @Query("""
           select k
             from TenantIntegrationKey k
            where k.tenant.id = :tenantId
              and k.isDeleted = false
           """)
    Page<TenantIntegrationKey> findByTenantIdAndIsDeletedFalse(@Param("tenantId") Integer tenantId,
                                                              Pageable pageable);

    @Query("""
           select k
             from TenantIntegrationKey k
            where k.tenant.id = :tenantId
              and k.status    = :status
              and k.isDeleted = false
           """)
    List<TenantIntegrationKey> findByTenantIdAndStatusAndIsDeletedFalse(@Param("tenantId") Integer tenantId,
                                                                       @Param("status") Status status);

    @Query("""
           select count(k) > 0
             from TenantIntegrationKey k
            where k.tenant.id = :tenantId
              and k.keyId     = :keyId
              and k.isDeleted = false
           """)
    boolean existsByTenantIdAndKeyIdAndIsDeletedFalse(@Param("tenantId") Integer tenantId,
                                                      @Param("keyId") String keyId);

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