package com.lms.setup.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
@Entity
@DynamicUpdate
@Table(
    name = "country",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_country_cd", columnNames = {"country_cd"})
    },
    indexes = {
        @Index(name = "idx_country_cd", columnList = "country_cd"),
        @Index(name = "idx_country_en_nm", columnList = "country_en_nm")
    }
)
public class Country implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "country_id", unique = true, nullable = false)
    private Integer countryId;

    @NotBlank
    @Size(max = 3) // ISO alpha-2 or alpha-3; relax if you need longer
    @Column(name = "country_cd", length = 3, nullable = false)
    private String countryCd;

    @NotBlank
    @Size(max = 256)
    @Column(name = "country_en_nm", length = 256, nullable = false)
    private String countryEnNm;

    @NotBlank
    @Size(max = 256)
    @Column(name = "country_ar_nm", length = 256, nullable = false)
    private String countryArNm;

    @Size(max = 10)
    @Column(name = "dialing_code", length = 10)
    private String dialingCode;

    @Size(max = 256)
    @Column(name = "nationality_en", length = 256)
    private String nationalityEn;

    @Size(max = 256)
    @Column(name = "nationality_ar", length = 256)
    private String nationalityAr;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = Boolean.TRUE;

    @Size(max = 1000)
    @Column(name = "en_description", length = 1000)
    private String enDescription;

    @Size(max = 1000)
    @Column(name = "ar_description", length = 1000)
    private String arDescription;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Country() {}

    @PrePersist @PreUpdate
    private void normalize() {
        if (countryCd != null) countryCd = countryCd.trim();
        if (countryEnNm != null) countryEnNm = countryEnNm.trim();
        if (countryArNm != null) countryArNm = countryArNm.trim();
        if (dialingCode != null) dialingCode = dialingCode.trim();
        if (nationalityEn != null) nationalityEn = nationalityEn.trim();
        if (nationalityAr != null) nationalityAr = nationalityAr.trim();
        if (isActive == null) isActive = Boolean.TRUE;
    }

    // getters/setters
    @JsonProperty("countryId")
    public Integer getCountryId() { return countryId; }
    public void setCountryId(Integer countryId) { this.countryId = countryId; }

    public String getCountryCd() { return countryCd; }
    public void setCountryCd(String countryCd) { this.countryCd = countryCd; }

    public String getCountryEnNm() { return countryEnNm; }
    public void setCountryEnNm(String countryEnNm) { this.countryEnNm = countryEnNm; }

    public String getCountryArNm() { return countryArNm; }
    public void setCountryArNm(String countryArNm) { this.countryArNm = countryArNm; }

    public String getDialingCode() { return dialingCode; }
    public void setDialingCode(String dialingCode) { this.dialingCode = dialingCode; }

    public String getNationalityEn() { return nationalityEn; }
    public void setNationalityEn(String nationalityEn) { this.nationalityEn = nationalityEn; }

    public String getNationalityAr() { return nationalityAr; }
    public void setNationalityAr(String nationalityAr) { this.nationalityAr = nationalityAr; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean active) { isActive = active; }

    public String getEnDescription() { return enDescription; }
    public void setEnDescription(String enDescription) { this.enDescription = enDescription; }

    public String getArDescription() { return arDescription; }
    public void setArDescription(String arDescription) { this.arDescription = arDescription; }

    public Long getVersion() { return version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Country that)) return false;
        return Objects.equals(countryId, that.countryId);
    }
    @Override public int hashCode() { return Objects.hash(countryId); }
    @Override public String toString() {
        return "Country{countryId=" + countryId + ", countryCd='" + countryCd + "'}";
    }
}
