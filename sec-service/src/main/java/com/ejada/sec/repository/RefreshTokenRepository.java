package com.ejada.sec.repository;

import com.ejada.sec.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

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
