package com.ejada.sec.repository;

import com.ejada.sec.domain.EventSeverity;
import com.ejada.sec.domain.SecurityEvent;
import com.ejada.sec.domain.SecurityEventType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SecurityEventRepository extends JpaRepository<SecurityEvent, Long> {

    @Query("SELECT e.ipAddress FROM SecurityEvent e "
            + "WHERE e.userId = :userId "
            + "AND e.eventType = 'LOGIN_SUCCESS' "
            + "ORDER BY e.eventTime DESC")
    List<String> findRecentSuccessfulLoginIps(@Param("userId") Long userId, Pageable pageable);

    default List<String> findRecentSuccessfulLoginIps(Long userId, int limit) {
        return findRecentSuccessfulLoginIps(userId, PageRequest.of(0, limit));
    }

    @Query("SELECT COUNT(e) FROM SecurityEvent e "
            + "WHERE e.eventType = 'LOGIN_FAILURE' "
            + "AND (e.ipAddress = :ipAddress OR e.username = :identifier) "
            + "AND e.eventTime > :since")
    long countLoginFailuresSince(
            @Param("ipAddress") String ipAddress,
            @Param("identifier") String identifier,
            @Param("since") Instant since);

    @Query("SELECT e.ipAddress FROM SecurityEvent e "
            + "WHERE e.userId = :userId "
            + "AND e.eventType = 'LOGIN_SUCCESS' "
            + "AND e.eventTime > :since")
    List<String> findRecentLoginIpsForUser(
            @Param("userId") Long userId,
            @Param("since") Instant since);

    @Query("SELECT e FROM SecurityEvent e "
            + "WHERE e.flaggedForReview = true "
            + "AND e.reviewedAt IS NULL "
            + "ORDER BY e.riskScore DESC, e.eventTime DESC")
    Page<SecurityEvent> findUnreviewedHighRiskEvents(Pageable pageable);

    Page<SecurityEvent> findByEventTypeAndEventTimeBetween(
            SecurityEventType eventType, Instant startTime, Instant endTime, Pageable pageable);

    Page<SecurityEvent> findByUserIdAndEventTimeBetween(
            Long userId, Instant startTime, Instant endTime, Pageable pageable);

    Page<SecurityEvent> findByTenantIdAndSeverityIn(
            UUID tenantId, List<EventSeverity> severities, Pageable pageable);
}
