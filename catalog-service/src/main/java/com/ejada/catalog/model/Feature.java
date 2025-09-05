package com.ejada.catalog.model;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "feature",
    uniqueConstraints = @UniqueConstraint(name = "uk_feature_key", columnNames = "feature_key")
)
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Feature {

	 @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    @Column(name = "feature_id", nullable = false, updatable = false)
	    @EqualsAndHashCode.Include
	    private Integer featureId;

    @Column(name = "feature_key", length = 96, nullable = false)
    private String featureKey;                 // e.g. CATALOG.PRODUCTS, INVENTORY.WAREHOUSES

    @Column(name = "feature_en_nm", length = 128, nullable = false)
    private String featureEnNm;

    @Column(name = "feature_ar_nm", length = 128, nullable = false)
    private String featureArNm;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "category", length = 64)
    private String category;                   // logical grouping for UI/filtering

    @Column(name = "is_metered", nullable = false)
    private Boolean isMetered = Boolean.FALSE; // true => has usage

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = Boolean.TRUE;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = Boolean.FALSE;

    public boolean isActive()  { return Boolean.TRUE.equals(isActive); }
    public boolean isDeleted() { return Boolean.TRUE.equals(isDeleted); }
    public boolean isMetered() { return Boolean.TRUE.equals(isMetered); }

    public static Feature ref(final Integer id) {
        if (id == null) return null;
        Feature f = new Feature();
        f.setFeatureId(id);
        return f;
    }
    @Builder
    public Feature(final Integer featureId,
                   final String featureKey,
                   final String featureEnNm,
                   final String featureArNm,
                   final String description,
                   final String category,
                   final Boolean isMetered,
                   final Boolean isActive,
                   final Boolean isDeleted) {
        this.featureId   = featureId;
        this.featureKey  = featureKey;
        this.featureEnNm = featureEnNm;
        this.featureArNm = featureArNm;
        this.description = description;
        this.category    = category;
        this.isMetered   = (isMetered != null ? isMetered : Boolean.FALSE);
        this.isActive    = (isActive  != null ? isActive  : Boolean.TRUE);
        this.isDeleted   = (isDeleted != null ? isDeleted : Boolean.FALSE);
    }
}
