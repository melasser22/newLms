package com.lms.setup.model;

import jakarta.persistence.*;
import org.hibernate.annotations.DynamicUpdate;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    @Column(name = "city_cd", length = 50, nullable = false)
    private String cityCd;

    @Column(name = "city_en_nm", length = 200, nullable = false)
    private String cityEnNm;

    @Column(name = "city_ar_nm", length = 200, nullable = false)
    private String cityArNm;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "country_id", nullable = false)
    private Country country;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = Boolean.TRUE;

    public boolean isActive() { return Boolean.TRUE.equals(isActive); }
}
