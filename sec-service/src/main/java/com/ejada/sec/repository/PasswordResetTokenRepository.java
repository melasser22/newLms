package com.ejada.sec.repository;

import com.ejada.sec.domain.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenAndUsedAtIsNullAndExpiresAtAfter(String token, Instant now);
}
