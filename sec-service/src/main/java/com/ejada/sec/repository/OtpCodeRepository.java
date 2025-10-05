package com.ejada.sec.repository;

import com.ejada.sec.domain.OtpCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {

    /**
     * Finds active (unused, not expired) OTPs for user.
     */
    @Query("SELECT o FROM OtpCode o " +
           "WHERE o.user.id = :userId " +
           "AND o.usedAt IS NULL " +
           "AND o.expiresAt > :now " +
           "AND o.delivered = true " +
           "ORDER BY o.generatedAt DESC")
    List<OtpCode> findActiveOtpsByUserId(
        @Param("userId") Long userId,
        @Param("now") Instant now
    );

    /**
     * Deletes expired OTPs.
     */
    void deleteByExpiresAtBefore(Instant cutoff);
}
