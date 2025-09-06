package com.ejada.setup.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
@Entity
@DynamicUpdate
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
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
    @EqualsAndHashCode.Include
    @JsonProperty("countryId")
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

    @Builder.Default
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
    @Setter(AccessLevel.NONE)
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    @Setter(AccessLevel.NONE)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    @Setter(AccessLevel.NONE)
    private LocalDateTime updatedAt;

    @PrePersist @PreUpdate
    @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD", justification = "Called by JPA lifecycle")
    private void normalize() {
        if (countryCd != null) countryCd = countryCd.trim();
        if (countryEnNm != null) countryEnNm = countryEnNm.trim();
        if (countryArNm != null) countryArNm = countryArNm.trim();
        if (dialingCode != null) dialingCode = dialingCode.trim();
        if (nationalityEn != null) nationalityEn = nationalityEn.trim();
        if (nationalityAr != null) nationalityAr = nationalityAr.trim();
        if (isActive == null) isActive = Boolean.TRUE;
    }

    public static Country ref(final Long id) {
        Country country = new Country();
        if (id != null) {
            country.setCountryId(id.intValue());
        }
        return country;
    }

}
