package com.ejada.sec.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Records security-critical events for audit, compliance, and threat detection.
 */
@Entity
@Table(
        name = "security_events",
        indexes = {
            @Index(name = "ix_sec_events_user", columnList = "user_id"),
            @Index(name = "ix_sec_events_tenant", columnList = "tenant_id"),
            @Index(name = "ix_sec_events_type", columnList = "event_type"),
            @Index(name = "ix_sec_events_severity", columnList = "severity"),
            @Index(name = "ix_sec_events_time", columnList = "event_time"),
            @Index(name = "ix_sec_events_ip", columnList = "ip_address"),
            @Index(name = "ix_sec_events_flagged", columnList = "flagged_for_review")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecurityEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Tenant context (null for platform-level events). */
    @Column(name = "tenant_id")
    private UUID tenantId;

    /** User who triggered the event (null for anonymous events). */
    @Column(name = "user_id")
    private Long userId;

    /** Username at time of event (for audit trail if user deleted). */
    @Column(name = "username", length = 255)
    private String username;

    /** Type of security event. */
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private SecurityEventType eventType;

    /** Event severity level. */
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private EventSeverity severity;

    /** When the event occurred. */
    @Column(name = "event_time", nullable = false)
    @Builder.Default
    private Instant eventTime = Instant.now();

    /** Client IP address. */
    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    /** User agent string. */
    @Column(name = "user_agent", length = 512)
    private String userAgent;

    /** Resource accessed (e.g., "User", "Role"). */
    @Column(name = "resource", length = 100)
    private String resource;

    /** Action attempted (e.g., "CREATE", "UPDATE", "DELETE"). */
    @Column(name = "action", length = 50)
    private String action;

    /** Result status (SUCCESS, FAILURE, BLOCKED). */
    @Column(name = "status", length = 20)
    private String status;

    /** Additional event details as JSON. */
    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    /** Calculated risk score (0-100). */
    @Column(name = "risk_score")
    private Integer riskScore;

    /** Whether this event is flagged for security team review. */
    @Column(name = "flagged_for_review")
    @Builder.Default
    private Boolean flaggedForReview = false;

    /** When security team reviewed this event. */
    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    /** Who reviewed the event. */
    @Column(name = "reviewed_by")
    private Long reviewedBy;

    /** Review notes/comments. */
    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    /** Geographic location (city, country). */
    @Column(name = "location", length = 255)
    private String location;

    /** Device fingerprint hash. */
    @Column(name = "device_fingerprint", length = 255)
    private String deviceFingerprint;
}
