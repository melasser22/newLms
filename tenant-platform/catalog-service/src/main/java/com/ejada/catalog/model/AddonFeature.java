package com.ejada.catalog.model;

import jakarta.persistence.*;
import org.hibernate.annotations.DynamicUpdate;
import lombok.*;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.math.BigDecimal;

@Entity
@Table(
    name = "addon_feature",
    uniqueConstraints = @UniqueConstraint(name = "uk_addon_feature", columnNames = {"addon_id","feature_id"}),
    indexes = {
        @Index(name = "idx_af_enabled", columnList = "enabled"),
        @Index(name = "idx_af_enforcement", columnList = "enforcement")
    }
)
@DynamicUpdate
@Getter @Setter @NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AddonFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "addon_feature_id", nullable = false, updatable = false)
    @EqualsAndHashCode.Include
    private Integer addonFeatureId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "addon_id", nullable = false)
    @Getter(onMethod_ = @__(@SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Addon is a JPA entity")))
    @Setter(onMethod_ = @__(@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Addon is a JPA entity")))
    private Addon addon;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "feature_id", nullable = false)
    @Getter(onMethod_ = @__(@SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Feature is a JPA entity")))
    @Setter(onMethod_ = @__(@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Feature is a JPA entity")))
    private Feature feature;

    // Policy & limits (keep parity with TierFeature)
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = Boolean.TRUE;

    @Enumerated(EnumType.STRING)
    @Column(name = "enforcement", length = 24, nullable = false)
    private Enforcement enforcement = Enforcement.ALLOW;

    @Column(name = "soft_limit", precision = 18, scale = 3)
    private BigDecimal softLimit;

    @Column(name = "hard_limit", precision = 18, scale = 3)
    private BigDecimal hardLimit;

    @Enumerated(EnumType.STRING)
    @Column(name = "limit_window", length = 24)
    private LimitWindow limitWindow;

    @Enumerated(EnumType.STRING)
    @Column(name = "measure_unit", length = 24)
    private MeasureUnit measureUnit;

    @Column(name = "reset_cron", length = 64)
    private String resetCron;

    // Overage
    @Column(name = "overage_enabled", nullable = false)
    private Boolean overageEnabled = Boolean.FALSE;

    @Column(name = "overage_unit_price", precision = 18, scale = 4)
    private BigDecimal overageUnitPrice;

    @Column(name = "overage_currency", length = 3)
    private String overageCurrency = "SAR";

    @Column(name = "meta", columnDefinition = "jsonb")
    private String meta;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = Boolean.FALSE;

    public boolean isEnabled()        { return Boolean.TRUE.equals(enabled); }
    public boolean isDeleted()        { return Boolean.TRUE.equals(isDeleted); }
    public boolean isOverageEnabled() { return Boolean.TRUE.equals(overageEnabled); }

    /** id-only helpers for builder ergonomics */
    public static AddonFeature ref(final Integer id) {
        if (id == null) return null;
        AddonFeature x = new AddonFeature();
        x.setAddonFeatureId(id);
        return x;
    }

    @Builder
    public AddonFeature(Integer addonFeatureId, Integer addonId, Integer featureId,
                        Boolean enabled, Enforcement enforcement,
                        BigDecimal softLimit, BigDecimal hardLimit,
                        LimitWindow limitWindow, MeasureUnit measureUnit, String resetCron,
                        Boolean overageEnabled, BigDecimal overageUnitPrice, String overageCurrency,
                        String meta, Boolean isDeleted) {
        this.addonFeatureId  = addonFeatureId;
        this.addon           = Addon.ref(addonId);
        this.feature         = Feature.ref(featureId);
        this.enabled         = enabled != null ? enabled : Boolean.TRUE;
        this.enforcement     = enforcement != null ? enforcement : Enforcement.ALLOW;
        this.softLimit       = softLimit;
        this.hardLimit       = hardLimit;
        this.limitWindow     = limitWindow;
        this.measureUnit     = measureUnit;
        this.resetCron       = resetCron;
        this.overageEnabled  = overageEnabled != null ? overageEnabled : Boolean.FALSE;
        this.overageUnitPrice = overageUnitPrice;
        this.overageCurrency = overageCurrency != null ? overageCurrency : "SAR";
        this.meta            = meta;
        this.isDeleted       = isDeleted != null ? isDeleted : Boolean.FALSE;
    }

    @PrePersist @PreUpdate
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
}
