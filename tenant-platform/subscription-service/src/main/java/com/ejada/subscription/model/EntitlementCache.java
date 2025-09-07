package com.ejada.subscription.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(
    name = "entitlement_cache",
    uniqueConstraints = @UniqueConstraint(name = "uk_entitlement", columnNames = {"subscription_id","feature_key"}),
    indexes = {
        @Index(name = "idx_ec_sub", columnList = "subscription_id"),
        @Index(name = "idx_ec_feature", columnList = "feature_key")
    }
)
@DynamicUpdate
@Getter @Setter @NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class EntitlementCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "entitlement_cache_id", nullable = false, updatable = false)
    @EqualsAndHashCode.Include
    private Long entitlementCacheId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(name = "feature_key", length = 96, nullable = false)
    private String featureKey;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = Boolean.TRUE;

    @Column(name = "enforcement", length = 24, nullable = false)
    private String enforcement; // ALLOW | BLOCK | ALLOW_WITH_OVERAGE

    @Column(name = "soft_limit", precision = 18, scale = 3)
    private BigDecimal softLimit;

    @Column(name = "hard_limit", precision = 18, scale = 3)
    private BigDecimal hardLimit;

    @Column(name = "limit_window", length = 24)
    private String limitWindow;

    @Column(name = "measure_unit", length = 24)
    private String measureUnit;

    @Column(name = "overage_enabled", nullable = false)
    private Boolean overageEnabled = Boolean.FALSE;

    @Column(name = "overage_unit_price", precision = 18, scale = 4)
    private BigDecimal overageUnitPrice;

    @Column(name = "overage_currency", length = 3)
    private String overageCurrency;

    @Column(name = "effective_from", nullable = false)
    private OffsetDateTime effectiveFrom = OffsetDateTime.now();

    @Column(name = "effective_to")
    private OffsetDateTime effectiveTo;

    @Column(name = "source_meta", columnDefinition = "jsonb")
    private String sourceMeta;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public static EntitlementCache ref(Long id) {
        if (id == null) return null;
        EntitlementCache x = new EntitlementCache();
        x.setEntitlementCacheId(id);
        return x;
    }
}
