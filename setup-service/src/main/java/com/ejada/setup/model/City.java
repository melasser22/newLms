package com.ejada.setup.model;

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
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.DynamicUpdate;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Entity
@Table(
    name = "city",
    uniqueConstraints = @UniqueConstraint(name = "uk_city_cd", columnNames = "city_cd"),
    indexes = {
        @Index(name = "idx_city_country_active", columnList = "country_id,is_active"),
        @Index(name = "idx_city_en_nm", columnList = "city_en_nm"),
        @Index(name = "idx_city_ar_nm", columnList = "city_ar_nm")
    }
)
@DynamicUpdate
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class City {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "city_id", nullable = false, updatable = false)
    @EqualsAndHashCode.Include
    private Integer cityId;

    private static final int CODE_LENGTH = 50;
    private static final int NAME_LENGTH = 200;

    @Column(name = "city_cd", length = CODE_LENGTH, nullable = false)
    private String cityCd;

    @Column(name = "city_en_nm", length = NAME_LENGTH, nullable = false)
    private String cityEnNm;

    @Column(name = "city_ar_nm", length = NAME_LENGTH, nullable = false)
    private String cityArNm;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "country_id", nullable = false)
    @Getter(onMethod_ = @__(@SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Country is a JPA entity")))
    @Setter(onMethod_ = @__(@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Country is a JPA entity")))
    private Country country;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = Boolean.TRUE;

    public final boolean isActive() {
        return Boolean.TRUE.equals(isActive);
    }

    @Builder
    public City(final Integer cityId, final String cityCd, final String cityEnNm,
                final String cityArNm, final Long countryId, final Boolean isActive) {
        this.cityId = cityId;
        this.cityCd = cityCd;
        this.cityEnNm = cityEnNm;
        this.cityArNm = cityArNm;
        this.country = countryId != null ? Country.ref(countryId) : null;
        this.isActive = isActive;
    }
}
