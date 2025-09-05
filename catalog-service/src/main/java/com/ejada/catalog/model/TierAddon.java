package com.ejada.catalog.model;

import jakarta.persistence.*;
import org.hibernate.annotations.DynamicUpdate;
import lombok.*;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.math.BigDecimal;

@Entity
@Table(
    name = "tier_addon",
    uniqueConstraints = @UniqueConstraint(name = "uk_tier_addon", columnNames = {"tier_id","addon_id"}),
    indexes = {
        @Index(name = "idx_ta_included", columnList = "included")
    }
)
@DynamicUpdate
@Getter @Setter @NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TierAddon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tier_addon_id", nullable = false, updatable = false)
    @EqualsAndHashCode.Include
    private Integer tierAddonId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tier_id", nullable = false)
    @Getter(onMethod_ = @__(@SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Tier is a JPA entity")))
    @Setter(onMethod_ = @__(@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Tier is a JPA entity")))
    private Tier tier;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "addon_id", nullable = false)
    @Getter(onMethod_ = @__(@SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Addon is a JPA entity")))
    @Setter(onMethod_ = @__(@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Addon is a JPA entity")))
    private Addon addon;

    @Column(name = "included", nullable = false)
    private Boolean included = Boolean.FALSE;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    // optional catalog list price (billing will own charging)
    @Column(name = "base_price", precision = 18, scale = 4)
    private BigDecimal basePrice;

    @Column(name = "currency", length = 3)
    private String currency;


    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = Boolean.FALSE;

    public boolean isIncluded() { return Boolean.TRUE.equals(included); }
    public boolean isDeleted()  { return Boolean.TRUE.equals(isDeleted); }

    /** id-only helpers */
    public static TierAddon ref(final Integer id) {
        if (id == null) return null;
        TierAddon ta = new TierAddon();
        ta.setTierAddonId(id);
        return ta;
    }

    @Builder
    public TierAddon(Integer tierAddonId, Integer tierId, Integer addonId,
                     Boolean included, Integer sortOrder,
                     BigDecimal basePrice, String currency, 
                     Boolean isDeleted) {
        this.tierAddonId   = tierAddonId;
        this.tier          = Tier.ref(tierId);
        this.addon         = Addon.ref(addonId);
        this.included      = included != null ? included : Boolean.FALSE;
        this.sortOrder     = sortOrder != null ? sortOrder : 0;
        this.basePrice     = basePrice;
        this.currency      = currency;
        this.isDeleted     = isDeleted != null ? isDeleted : Boolean.FALSE;
    }
}
