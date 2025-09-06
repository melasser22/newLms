package com.ejada.catalog.model;

import jakarta.persistence.*;
import org.hibernate.annotations.DynamicUpdate;
import lombok.*;

@Entity
@Table(
    name = "addon",
    uniqueConstraints = @UniqueConstraint(name = "uk_addon_cd", columnNames = "addon_cd"),
    indexes = {
        @Index(name = "idx_addon_active", columnList = "is_active"),
        @Index(name = "idx_addon_category", columnList = "category"),
        @Index(name = "idx_addon_en_nm", columnList = "addon_en_nm"),
        @Index(name = "idx_addon_ar_nm", columnList = "addon_ar_nm")
    }
)
@DynamicUpdate
@Getter @Setter @NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Addon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "addon_id", nullable = false, updatable = false)
    @EqualsAndHashCode.Include
    private Integer addonId;

    @Column(name = "addon_cd", length = 64, nullable = false)
    private String addonCd;

    @Column(name = "addon_en_nm", length = 128, nullable = false)
    private String addonEnNm;

    @Column(name = "addon_ar_nm", length = 128, nullable = false)
    private String addonArNm;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "category", length = 64)
    private String category;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = Boolean.TRUE;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = Boolean.FALSE;

    public boolean isActive()  { return Boolean.TRUE.equals(isActive); }
    public boolean isDeleted() { return Boolean.TRUE.equals(isDeleted); }


    /** id-only reference helper */
    public static Addon ref(final Integer id) {
        if (id == null) return null;
        Addon a = new Addon();
        a.setAddonId(id);
        return a;
    }

    @Builder
    public Addon(Integer addonId, String addonCd, String addonEnNm, String addonArNm,
                 String description, String category, Boolean isActive, Boolean isDeleted) {
        this.addonId   = addonId;
        this.addonCd   = addonCd;
        this.addonEnNm = addonEnNm;
        this.addonArNm = addonArNm;
        this.description = description;
        this.category  = category;
        this.isActive  = isActive != null ? isActive : Boolean.TRUE;
        this.isDeleted = isDeleted != null ? isDeleted : Boolean.FALSE;
    }
}
