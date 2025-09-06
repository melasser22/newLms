package com.ejada.catalog.model;

import jakarta.persistence.*;
import org.hibernate.annotations.DynamicUpdate;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.math.BigDecimal;

@Entity
@Table(
    name = "tier_feature",
    uniqueConstraints = @UniqueConstraint(name = "uk_tier_feature", columnNames = {"tier_id", "feature_id"}),
    indexes = {
        @Index(name = "idx_tf_enabled", columnList = "enabled"),
        @Index(name = "idx_tf_enforcement", columnList = "enforcement")
    }
)
@DynamicUpdate
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TierFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tier_feature_id", nullable = false, updatable = false)
    @EqualsAndHashCode.Include
    private Integer tierFeatureId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tier_id", nullable = false)
    @Getter(onMethod_ = @__(@SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Tier is a JPA entity")))
    @Setter(onMethod_ = @__(@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Tier is a JPA entity")))
    private Tier tier;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "feature_id", nullable = false)
    @Getter(onMethod_ = @__(@SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Feature is a JPA entity")))
    @Setter(onMethod_ = @__(@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Feature is a JPA entity")))
    private Feature feature;

    // Policy
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = Boolean.TRUE;

    @Enumerated(EnumType.STRING)
    @Column(name = "enforcement", length = 24, nullable = false)
    private Enforcement enforcement = Enforcement.ALLOW; // ALLOW | BLOCK | ALLOW_WITH_OVERAGE

    // Limits
    @Column(name = "soft_limit", precision = 18, scale = 3)
    private BigDecimal softLimit;

    @Column(name = "hard_limit", precision = 18, scale = 3)
    private BigDecimal hardLimit;

    @Enumerated(EnumType.STRING)
    @Column(name = "limit_window", length = 24)
    private LimitWindow limitWindow; // DAILY | MONTHLY | QUARTERLY | YEARLY | LIFETIME

    @Enumerated(EnumType.STRING)
    @Column(name = "measure_unit", length = 24)
    private MeasureUnit measureUnit; // REQUESTS | ITEMS | MB | GB | POINTS ...

    @Column(name = "reset_cron", length = 64)
    private String resetCron;

    // Overage
    @Column(name = "overage_enabled", nullable = false)
    private Boolean overageEnabled = Boolean.FALSE;

    @Column(name = "overage_unit_price", precision = 18, scale = 4)
    private BigDecimal overageUnitPrice; // required when overageEnabled = true

    @Column(name = "overage_currency", length = 3)
    private String overageCurrency = "SAR";

    // Meta / soft-delete
    @Column(name = "meta", columnDefinition = "jsonb")
    private String meta;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = Boolean.FALSE;

    public boolean isEnabled()         { return Boolean.TRUE.equals(enabled); }
    public boolean isDeleted()         { return Boolean.TRUE.equals(isDeleted); }
    public boolean isOverageEnabled()  { return Boolean.TRUE.equals(overageEnabled); }

    @Builder
    public TierFeature(final Integer tierFeatureId,
                       final Integer tierId,
                       final Integer featureId,
                       final Boolean enabled,
                       final Enforcement enforcement,
                       final BigDecimal softLimit,
                       final BigDecimal hardLimit,
                       final LimitWindow limitWindow,
                       final MeasureUnit measureUnit,
                       final String resetCron,
                       final Boolean overageEnabled,
                       final BigDecimal overageUnitPrice,
                       final String overageCurrency,
                       final String meta,
                       final Boolean isDeleted) {

        this.tierFeatureId   = tierFeatureId;
        this.tier            = (tierId != null ? Tier.ref(tierId) : null);
        this.feature         = (featureId != null ? Feature.ref(featureId) : null);

        this.enabled         = (enabled != null ? enabled : Boolean.TRUE);
        this.enforcement     = (enforcement != null ? enforcement : Enforcement.ALLOW);

        this.softLimit       = softLimit;
        this.hardLimit       = hardLimit;
        this.limitWindow     = limitWindow;
        this.measureUnit     = measureUnit;
        this.resetCron       = resetCron;

        this.overageEnabled  = (overageEnabled != null ? overageEnabled : Boolean.FALSE);
        this.overageUnitPrice = overageUnitPrice;
        this.overageCurrency = (overageCurrency != null ? overageCurrency : "SAR");

        this.meta            = meta;
        this.isDeleted       = (isDeleted != null ? isDeleted : Boolean.FALSE);
    }

    /* --- Optional guard methods (domain validation) --- */

    private void validatePolicy() {
        if (enforcement == Enforcement.BLOCK && hardLimit == null) {
            throw new IllegalStateException("hardLimit is required when enforcement=BLOCK");
        }
        if (overageEnabled && overageUnitPrice == null) {
            throw new IllegalStateException("overageUnitPrice is required when overageEnabled=true");
        }
        if (softLimit != null && hardLimit != null && hardLimit.compareTo(softLimit) < 0) {
            throw new IllegalStateException("hardLimit must be >= softLimit");
        }
    }

    @PrePersist
    @PreUpdate
    private void jpaValidate() {
        validatePolicy();
    }
}
