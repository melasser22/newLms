package com.ejada.setup.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Locale;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
@Entity
@DynamicUpdate
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(
    name = "resources",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_resource_cd", columnNames = {"resource_cd"})
    },
    indexes = {
        @Index(name = "idx_resource_cd", columnList = "resource_cd"),
        @Index(name = "idx_resource_parent", columnList = "parent_resource_id")
    }
)
public class Resource implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "resource_id",  unique = true, nullable = false)
    @EqualsAndHashCode.Include
    private Integer resourceId;

    @NotBlank
    @Size(max = 128)
    @Column(name = "resource_cd", length = 128, nullable = false)
    private String resourceCd;

    @NotBlank
    @Size(max = 256)
    @Column(name = "resource_en_nm", length = 256, nullable = false)
    private String resourceEnNm;

    @NotBlank
    @Size(max = 256)
    @Column(name = "resource_ar_nm", length = 256, nullable = false)
    private String resourceArNm;

    @Size(max = 512)
    @Column(name = "path", length = 512)
    private String path;

    @Size(max = 16)
    @Column(name = "http_method", length = 16)
    private String httpMethod;

    @Column(name = "parent_resource_id")
    private Integer parentResourceId;

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
        if (resourceCd != null) resourceCd = resourceCd.trim();
        if (resourceEnNm != null) resourceEnNm = resourceEnNm.trim();
        if (resourceArNm != null) resourceArNm = resourceArNm.trim();
        if (path != null) path = path.trim();
        if (httpMethod != null) httpMethod = httpMethod.trim().toUpperCase(Locale.ROOT);
        if (isActive == null) isActive = Boolean.TRUE;
    }

}
