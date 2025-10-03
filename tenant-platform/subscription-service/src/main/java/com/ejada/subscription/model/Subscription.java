package com.ejada.subscription.model;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(
    name = "subscription",
    uniqueConstraints = @UniqueConstraint(name = "ux_sub_unique_ext", columnNames = {"ext_subscription_id", "ext_customer_id"}),
    indexes = {
        @Index(name = "idx_sub_ext_customer", columnList = "ext_customer_id"),
        @Index(name = "idx_sub_ext_product", columnList = "ext_product_id"),
        @Index(name = "idx_sub_status_cd", columnList = "subscription_stts_cd")
    }
)
@DynamicUpdate
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subscription_id", nullable = false, updatable = false)
    @EqualsAndHashCode.Include
    private Long subscriptionId;

    // External ids from marketplace
    @Column(name = "ext_subscription_id", nullable = false)
    private Long extSubscriptionId;

    @Column(name = "ext_customer_id", nullable = false)
    private Long extCustomerId;

    @Column(name = "ext_product_id", nullable = false)
    private Long extProductId;

    @Column(name = "ext_tier_id", nullable = false)
    private Long extTierId;

    @Column(name = "tier_nm_en", length = 256)
    private String tierNmEn;

    @Column(name = "tier_nm_ar", length = 256)
    private String tierNmAr;

    // Dates
    @Column(name = "start_dt", nullable = false)
    private LocalDate startDt;

    @Column(name = "end_dt", nullable = false)
    private LocalDate endDt;

    // Amounts
    @Column(name = "subscription_amount", precision = 18, scale = 4)
    private BigDecimal subscriptionAmount;

    @Column(name = "total_billed_amount", precision = 18, scale = 4)
    private BigDecimal totalBilledAmount;

    @Column(name = "total_paid_amount", precision = 18, scale = 4)
    private BigDecimal totalPaidAmount;

    // status & metadata
    @Column(name = "subscription_stts_cd", length = 8, nullable = false)
    private String subscriptionSttsCd;

    @Column(name = "approval_status", length = 32, nullable = false)
    private String approvalStatus = SubscriptionApprovalStatus.PENDING_APPROVAL.name();

    @Column(name = "approval_required")
    private Boolean approvalRequired = Boolean.TRUE;

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    @Column(name = "submitted_by", length = 128)
    private String submittedBy;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @Column(name = "approved_by", length = 128)
    private String approvedBy;

    @Column(name = "rejected_at")
    private OffsetDateTime rejectedAt;

    @Column(name = "rejected_by", length = 128)
    private String rejectedBy;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "admin_user_id")
    private Long adminUserId;

    @Column(name = "create_channel", length = 32)
    private String createChannel; // PORTAL | GCP_MARKETPLACE | ...

    // Limits (Y/N -> Boolean via converter)
    @Convert(converter = YesNoBooleanConverter.class)
    @Column(name = "unlimited_users_flag", length = 1)
    private Boolean unlimitedUsersFlag = Boolean.FALSE;

    @Column(name = "users_limit")
    private Long usersLimit;

    @Column(name = "users_limit_reset_type", length = 32)
    private String usersLimitResetType;

    @Convert(converter = YesNoBooleanConverter.class)
    @Column(name = "unlimited_trans_flag", length = 1)
    private Boolean unlimitedTransFlag = Boolean.FALSE;

    @Column(name = "transactions_limit")
    private Long transactionsLimit;

    @Column(name = "trans_limit_reset_type", length = 32)
    private String transLimitResetType;

    @Column(name = "balance_limit", precision = 18, scale = 4)
    private BigDecimal balanceLimit;

    @Column(name = "balance_limit_reset_type", length = 32)
    private String balanceLimitResetType;

    @Column(name = "environment_size_cd", length = 8)
    private String environmentSizeCd; // L | XL

    // auto provisioning + previous link
    @Convert(converter = YesNoBooleanConverter.class)
    @Column(name = "is_auto_prov_enabled", length = 1)
    private Boolean isAutoProvEnabled = Boolean.FALSE;

    @Column(name = "prev_subscription_id")
    private Long prevSubscriptionId;

    @Column(name = "prev_subscription_update_action", length = 16)
    private String prevSubscriptionUpdateAction; // UPGRADE | DOWNGRADE | RENEWAL

    @Column(name = "tenant_code", length = 64)
    private String tenantCode;

    @Column(name = "security_tenant_id")
    private java.util.UUID securityTenantId;

    // housekeeping
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "meta", columnDefinition = "jsonb")
    private String meta;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = Boolean.FALSE;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;

    /** id-only ref helper */
    public static Subscription ref(final Long id) {
        if (id == null) {
            return null;
        }
        Subscription s = new Subscription();
        s.setSubscriptionId(id);
        return s;
    }
}
