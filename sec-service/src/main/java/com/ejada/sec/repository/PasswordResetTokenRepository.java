package com.ejada.sec.repository;

import com.ejada.sec.domain.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenAndUsedAtIsNullAndExpiresAtAfter(String token, Instant now);

    @Modifying
    @Query("update PasswordResetToken t set t.usedAt = :now, t.expiresAt = :now where t.user.id = :userId and t.usedAt is null and t.expiresAt > :now")
    int invalidateActiveTokens(@Param("userId") Long userId, @Param("now") Instant now);

    @Modifying
    int deleteByUserId(Long userId);
}
