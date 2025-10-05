package com.ejada.sec.service;

import com.ejada.sec.domain.EventSeverity;
import com.ejada.sec.domain.SecurityEvent;
import com.ejada.sec.domain.SecurityEventType;
import com.ejada.sec.domain.User;
import com.ejada.sec.repository.SecurityEventRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for logging and analyzing security events.
 * Performs real-time anomaly detection and alerting.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityEventService {

    private final SecurityEventRepository eventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Logs a security event asynchronously with risk scoring.
     *
     * @param event the security event to log
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logEvent(SecurityEvent event) {
        if (event.getRiskScore() == null) {
            event.setRiskScore(calculateRiskScore(event));
        }

        if (event.getRiskScore() >= 75) {
            event.setFlaggedForReview(true);
        }

        eventRepository.save(event);

        kafkaTemplate.send("security-events", event.getEventType().name(), event);

        log.info(
                "Security event logged: type={}, user={}, severity={}, risk={}",
                event.getEventType(),
                event.getUserId(),
                event.getSeverity(),
                event.getRiskScore());

        detectAnomalies(event);
    }

    /**
     * Logs a login failure and checks for brute force.
     */
    public void logLoginFailure(UUID tenantId, String identifier, String ipAddress, String userAgent) {
        SecurityEvent event = SecurityEvent.builder()
                .tenantId(tenantId)
                .username(identifier)
                .eventType(SecurityEventType.LOGIN_FAILURE)
                .severity(EventSeverity.MEDIUM)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .status("FAILURE")
                .build();

        logEvent(event);

        long recentFailures = countRecentFailures(ipAddress, identifier, 15);
        if (recentFailures >= 5) {
            logBruteForceDetection(tenantId, identifier, ipAddress);
        }
    }

    /**
     * Logs a successful login and checks for anomalies.
     */
    public void logLoginSuccess(User user, String ipAddress, String userAgent, String location) {
        SecurityEvent event = SecurityEvent.builder()
                .tenantId(user.getTenantId())
                .userId(user.getId())
                .username(user.getUsername())
                .eventType(SecurityEventType.LOGIN_SUCCESS)
                .severity(EventSeverity.INFO)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .location(location)
                .status("SUCCESS")
                .build();

        logEvent(event);

        if (user.getId() != null && isUnusualLocation(user.getId(), ipAddress)) {
            logSuspiciousLocation(user, ipAddress, location);
        }
    }

    /**
     * Logs privilege escalation attempt.
     */
    public void logPrivilegeEscalation(Long userId, String username, String attemptedAction, String details) {
        SecurityEvent event = SecurityEvent.builder()
                .userId(userId)
                .username(username)
                .eventType(SecurityEventType.PRIVILEGE_ESCALATION_ATTEMPT)
                .severity(EventSeverity.CRITICAL)
                .action(attemptedAction)
                .status("BLOCKED")
                .details(details)
                .flaggedForReview(true)
                .build();

        logEvent(event);
    }

    private void detectAnomalies(SecurityEvent event) {
        if (event.getUserId() == null) {
            return;
        }

        if (event.getEventType() == SecurityEventType.LOGIN_SUCCESS) {
            checkConcurrentSessionAnomaly(event.getUserId(), event.getIpAddress());
        }

        if (isUnusualTime(event.getEventTime())) {
            flagUnusualTimeAccess(event);
        }
    }

    private int calculateRiskScore(SecurityEvent event) {
        int score = event.getSeverity().getScore();

        if ("FAILURE".equals(event.getStatus()) || "BLOCKED".equals(event.getStatus())) {
            score += 15;
        }

        if (Boolean.TRUE.equals(event.getFlaggedForReview())) {
            score += 20;
        }

        if (event.getEventType() == SecurityEventType.BRUTE_FORCE_DETECTED
                || event.getEventType() == SecurityEventType.PRIVILEGE_ESCALATION_ATTEMPT) {
            score += 25;
        }

        return Math.min(score, 100);
    }

    private long countRecentFailures(String ipAddress, String identifier, int minutesBack) {
        Instant since = Instant.now().minus(minutesBack, ChronoUnit.MINUTES);
        return eventRepository.countLoginFailuresSince(ipAddress, identifier, since);
    }

    private boolean isUnusualLocation(Long userId, String ipAddress) {
        List<String> recentIps = eventRepository.findRecentSuccessfulLoginIps(userId, 10);
        return !recentIps.contains(ipAddress);
    }

    private boolean isUnusualTime(Instant eventTime) {
        return false;
    }

    private void logBruteForceDetection(UUID tenantId, String identifier, String ipAddress) {
        SecurityEvent event = SecurityEvent.builder()
                .tenantId(tenantId)
                .username(identifier)
                .eventType(SecurityEventType.BRUTE_FORCE_DETECTED)
                .severity(EventSeverity.CRITICAL)
                .ipAddress(ipAddress)
                .status("DETECTED")
                .details("5+ failed login attempts in 15 minutes")
                .flaggedForReview(true)
                .build();

        logEvent(event);
    }

    private void logSuspiciousLocation(User user, String ipAddress, String location) {
        SecurityEvent event = SecurityEvent.builder()
                .tenantId(user.getTenantId())
                .userId(user.getId())
                .username(user.getUsername())
                .eventType(SecurityEventType.SUSPICIOUS_LOCATION)
                .severity(EventSeverity.HIGH)
                .ipAddress(ipAddress)
                .location(location)
                .status("FLAGGED")
                .details("Login from new location")
                .flaggedForReview(true)
                .build();

        logEvent(event);
    }

    private void checkConcurrentSessionAnomaly(Long userId, String currentIp) {
        Instant since = Instant.now().minus(5, ChronoUnit.MINUTES);
        List<String> recentIps = eventRepository.findRecentLoginIpsForUser(userId, since);
        Set<String> uniqueIps = new HashSet<>(recentIps);

        if (uniqueIps.size() > 2) {
            SecurityEvent event = SecurityEvent.builder()
                    .userId(userId)
                    .eventType(SecurityEventType.CONCURRENT_SESSION_ANOMALY)
                    .severity(EventSeverity.CRITICAL)
                    .ipAddress(currentIp)
                    .status("DETECTED")
                    .details("Multiple IPs: " + String.join(", ", uniqueIps))
                    .flaggedForReview(true)
                    .build();

            logEvent(event);
        }
    }

    private void flagUnusualTimeAccess(SecurityEvent event) {
        SecurityEvent anomaly = SecurityEvent.builder()
                .tenantId(event.getTenantId())
                .userId(event.getUserId())
                .username(event.getUsername())
                .eventType(SecurityEventType.UNUSUAL_TIME_ACCESS)
                .severity(EventSeverity.MEDIUM)
                .ipAddress(event.getIpAddress())
                .status("FLAGGED")
                .details("Access outside business hours")
                .build();

        logEvent(anomaly);
    }
}
