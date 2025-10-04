package com.ejada.sec.repository;

import com.ejada.data.repository.TenantAwareRepository;
import com.ejada.sec.domain.RefreshToken;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends TenantAwareRepository<RefreshToken, Long> {

    @Override
    @Query("SELECT rt FROM RefreshToken rt JOIN rt.user u " +
           "WHERE rt.id = :id AND u.tenantId = :tenantId")
    Optional<RefreshToken> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") UUID tenantId);

    @Override
    @Query("SELECT CASE WHEN COUNT(rt) > 0 THEN true ELSE false END FROM RefreshToken rt " +
           "JOIN rt.user u WHERE rt.id = :id AND u.tenantId = :tenantId")
    boolean existsByIdAndTenantId(@Param("id") Long id, @Param("tenantId") UUID tenantId);

    @Override
    @Query("SELECT rt FROM RefreshToken rt JOIN rt.user u WHERE u.tenantId = :tenantId")
    List<RefreshToken> findAllByTenantId(@Param("tenantId") UUID tenantId);

    @Override
    @Query("SELECT rt FROM RefreshToken rt JOIN rt.user u WHERE u.tenantId = :tenantId")
    List<RefreshToken> findAllByTenantId(@Param("tenantId") UUID tenantId, Sort sort);

    @Override
    @Query("SELECT rt FROM RefreshToken rt JOIN rt.user u WHERE u.tenantId = :tenantId")
    Page<RefreshToken> findAllByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Override
    @Query("SELECT COUNT(rt) FROM RefreshToken rt JOIN rt.user u WHERE u.tenantId = :tenantId")
    long countByTenantId(@Param("tenantId") UUID tenantId);

    @Override
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.id = :id AND rt.user.tenantId = :tenantId")
    void deleteByIdAndTenantId(@Param("id") Long id, @Param("tenantId") UUID tenantId);

    @Override
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.user.tenantId = :tenantId")
    void deleteAllByTenantId(@Param("tenantId") UUID tenantId);

    Optional<RefreshToken> findByToken(String token);

    @Query("SELECT rt FROM RefreshToken rt WHERE rt.user.id = :userId " +
           "AND rt.revokedAt IS NULL AND rt.expiresAt > :cutoff ORDER BY rt.issuedAt ASC")
    List<RefreshToken> findActiveTokensByUserId(@Param("userId") Long userId, @Param("cutoff") Instant cutoff);

    @Query("SELECT rt FROM RefreshToken rt WHERE rt.user.tenantId = :tenantId " +
           "AND rt.revokedAt IS NULL AND rt.expiresAt > :cutoff ORDER BY rt.issuedAt ASC")
    List<RefreshToken> findActiveTokensByTenant(@Param("tenantId") UUID tenantId,
                                                @Param("cutoff") Instant cutoff);

    @Modifying
    int deleteByUserId(Long userId);

    @Modifying
    int deleteByExpiresAtBefore(Instant cutoff);
}
