package com.ejada.catalog.model;

import jakarta.persistence.*;
import org.hibernate.annotations.DynamicUpdate;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "tier",
    uniqueConstraints = @UniqueConstraint(name = "uk_tier_cd", columnNames = "tier_cd"),
    indexes = {
        @Index(name = "idx_tier_active", columnList = "is_active"),
        @Index(name = "idx_tier_rank", columnList = "rank_order"),
        @Index(name = "idx_tier_en_nm", columnList = "tier_en_nm"),
        @Index(name = "idx_tier_ar_nm", columnList = "tier_ar_nm")
    }
)
@DynamicUpdate
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Tier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tier_id", nullable = false, updatable = false)
    @EqualsAndHashCode.Include
    private Integer tierId;

    @Column(name = "tier_cd", length = 64, nullable = false)
    private String tierCd;                    // e.g. BASIC, PRO, ENTERPRISE

    @Column(name = "tier_en_nm", length = 128, nullable = false)
    private String tierEnNm;

    @Column(name = "tier_ar_nm", length = 128, nullable = false)
    private String tierArNm;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "rank_order", nullable = false)
    private Integer rankOrder = 0;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = Boolean.TRUE;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = Boolean.FALSE;

    public boolean isActive()  { return Boolean.TRUE.equals(isActive); }
    public boolean isDeleted() { return Boolean.TRUE.equals(isDeleted); }


    public static Tier ref(final Integer id) {
        if (id == null) return null;
        Tier t = new Tier();
        t.setTierId(id);
        return t;
    }
    @Builder
    public Tier(final Integer tierId,
                final String tierCd,
                final String tierEnNm,
                final String tierArNm,
                final String description,
                final Integer rankOrder,
                final Boolean isActive,
                final Boolean isDeleted) {
        this.tierId = tierId;
        this.tierCd = tierCd;
        this.tierEnNm = tierEnNm;
        this.tierArNm = tierArNm;
        this.description = description;
        this.rankOrder = (rankOrder != null ? rankOrder : 0);
        this.isActive = (isActive != null ? isActive : Boolean.TRUE);
        this.isDeleted = (isDeleted != null ? isDeleted : Boolean.FALSE);
    }
}
