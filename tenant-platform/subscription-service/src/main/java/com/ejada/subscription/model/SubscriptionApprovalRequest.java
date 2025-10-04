package com.ejada.subscription.model;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "subscription_approval_request",
        indexes = {
            @Index(name = "idx_approval_request_status", columnList = "status"),
            @Index(name = "idx_approval_request_priority", columnList = "priority, status"),
            @Index(name = "idx_approval_request_due_date", columnList = "due_date")
        })
@DynamicUpdate
@Getter
@Setter
public class SubscriptionApprovalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "approval_request_id")
    private Long approvalRequestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(name = "request_type", length = 32, nullable = false)
    private String requestType = "NEW_SUBSCRIPTION";

    @Column(name = "status", length = 32, nullable = false)
    private String status = "PENDING";

    @Column(name = "priority", length = 16)
    private String priority = "NORMAL";

    @Column(name = "risk_score")
    private Integer riskScore;

    @Column(name = "risk_level", length = 16)
    private String riskLevel;

    @Column(name = "requested_at", nullable = false)
    private OffsetDateTime requestedAt = OffsetDateTime.now();

    @Column(name = "requested_by", length = 128)
    private String requestedBy;

    @Column(name = "due_date")
    private OffsetDateTime dueDate;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @Column(name = "approved_by", length = 128)
    private String approvedBy;

    @Column(name = "approver_email", length = 255)
    private String approverEmail;

    @Column(name = "approval_notes")
    private String approvalNotes;

    @Column(name = "rejected_at")
    private OffsetDateTime rejectedAt;

    @Column(name = "rejected_by", length = 128)
    private String rejectedBy;

    @Column(name = "rejection_reason", length = 64)
    private String rejectionReason;

    @Column(name = "rejection_notes")
    private String rejectionNotes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tenant_info_json", columnDefinition = "jsonb")
    private String tenantInfoJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "subscription_info_json", columnDefinition = "jsonb")
    private String subscriptionInfoJson;

    @Column(name = "email_verified")
    private Boolean emailVerified;

    @Column(name = "email_risk_score")
    private Integer emailRiskScore;

    @Column(name = "domain_age")
    private Integer domainAge;

    @Column(name = "domain_reputation", length = 32)
    private String domainReputation;

    @Column(name = "crm_customer_id", length = 64)
    private String crmCustomerId;

    @Column(name = "customer_segment", length = 32)
    private String customerSegment;

    @Column(name = "requires_additional_review")
    private Boolean requiresAdditionalReview = Boolean.FALSE;

    @Column(name = "processed_at")
    private OffsetDateTime processedAt;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = Boolean.FALSE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Subscription is managed JPA association")
    public Subscription getSubscription() {
        return subscription;
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Subscription lifecycle managed by JPA")
    public void setSubscription(final Subscription subscription) {
        this.subscription = subscription;
    }
}
