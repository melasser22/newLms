package com.lms.setup.model;

import jakarta.persistence.*;
import org.hibernate.annotations.DynamicUpdate;

import java.util.Objects;
import com.lms.setup.model.Country;

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
public class City {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "city_id", nullable = false, updatable = false)
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

    // Getters/Setters
    public Integer getCityId() { return cityId; }
    public void setCityId(Integer cityId) { this.cityId = cityId; }

    public String getCityCd() { return cityCd; }
    public void setCityCd(String cityCd) { this.cityCd = cityCd; }

    public String getCityEnNm() { return cityEnNm; }
    public void setCityEnNm(String cityEnNm) { this.cityEnNm = cityEnNm; }

    public String getCityArNm() { return cityArNm; }
    public void setCityArNm(String cityArNm) { this.cityArNm = cityArNm; }

    public Country getCountry() { return country; }
    public void setCountry(Country country) { this.country = country; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean active) { isActive = active; }
    public boolean isActive() { return Boolean.TRUE.equals(isActive); }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof City)) return false;
        City other = (City) o;
        return Objects.equals(cityId, other.cityId);
    }
    @Override public int hashCode() { return Objects.hash(cityId); }
}
