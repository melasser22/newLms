package com.ejada.sec.repository;

import com.ejada.data.repository.TenantAwareRepository;
import com.ejada.sec.domain.RefreshToken;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends TenantAwareRepository<RefreshToken, Long> {

    default Optional<RefreshToken> findByIdAndTenantId(Long id, UUID tenantId) {
        return findByIdAndUserTenantId(id, tenantId);
    }

    Optional<RefreshToken> findByIdAndUserTenantId(Long id, UUID tenantId);

    default List<RefreshToken> findAllByTenantId(UUID tenantId) {
        return findAllByUserTenantId(tenantId);
    }

    List<RefreshToken> findAllByUserTenantId(UUID tenantId);

    default void deleteByIdAndTenantId(Long id, UUID tenantId) {
        deleteByIdAndUserTenantId(id, tenantId);
    }

    void deleteByIdAndUserTenantId(Long id, UUID tenantId);

    Optional<RefreshToken> findByToken(String token);

    List<RefreshToken> findAllByUserId(Long userId);

    @Query("select t from RefreshToken t where t.user.id = :userId and t.revokedAt is null and t.expiresAt > :cutoff order by t.issuedAt asc")
    List<RefreshToken> findActiveTokensByUserId(@Param("userId") Long userId, @Param("cutoff") Instant cutoff);

    @Query("select t from RefreshToken t where t.user.tenantId = :tenantId and t.revokedAt is null and t.expiresAt > :cutoff order by t.issuedAt asc")
    List<RefreshToken> findActiveTokensByTenant(@Param("tenantId") UUID tenantId, @Param("cutoff") Instant cutoff);

    @Modifying
    int deleteByUserId(Long userId);

    @Modifying
    int deleteByExpiresAtBefore(Instant cutoff);
}
