package com.ejada.subscription.model;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "subscription_activity_log",
        indexes = {
            @Index(name = "idx_activity_log_subscription", columnList = "subscription_id, performed_at DESC"),
            @Index(name = "idx_activity_log_type", columnList = "activity_type, performed_at DESC"),
            @Index(name = "idx_activity_log_performed_by", columnList = "performed_by")
        })
@Getter
@Setter
public class SubscriptionActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "activity_log_id")
    private Long activityLogId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "subscription_id")
    private Subscription subscription;

    @Column(name = "activity_type", length = 64, nullable = false)
    private String activityType;

    @Column(name = "description")
    private String description;

    @Column(name = "performed_by", length = 128, nullable = false)
    private String performedBy;

    @CreationTimestamp
    @Column(name = "performed_at", nullable = false, updatable = false)
    private OffsetDateTime performedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Subscription is managed JPA association")
    public Subscription getSubscription() {
        return subscription;
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Subscription lifecycle managed by JPA")
    public void setSubscription(final Subscription subscription) {
        this.subscription = subscription;
    }
}
