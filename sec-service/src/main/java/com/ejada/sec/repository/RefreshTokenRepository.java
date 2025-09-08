package com.ejada.sec.repository;

import com.ejada.sec.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    List<RefreshToken> findAllByUserId(Long userId);

    @Modifying
    int deleteByUserId(Long userId);

    @Modifying
    int deleteByExpiresAtBefore(Instant cutoff);
}
