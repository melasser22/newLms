package com.ejada.setup.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
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
@AllArgsConstructor
@Builder
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

    private static final int CODE_LENGTH = 128;
    private static final int NAME_LENGTH = 256;
    private static final int PATH_LENGTH = 512;
    private static final int METHOD_LENGTH = 16;
    private static final int DESC_LENGTH = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "resource_id",  unique = true, nullable = false)
    @EqualsAndHashCode.Include
    private Integer resourceId;

    @NotBlank
    @Size(max = CODE_LENGTH)
    @Column(name = "resource_cd", length = CODE_LENGTH, nullable = false)
    private String resourceCd;

    @NotBlank
    @Size(max = NAME_LENGTH)
    @Column(name = "resource_en_nm", length = NAME_LENGTH, nullable = false)
    private String resourceEnNm;

    @NotBlank
    @Size(max = NAME_LENGTH)
    @Column(name = "resource_ar_nm", length = NAME_LENGTH, nullable = false)
    private String resourceArNm;

    @Size(max = PATH_LENGTH)
    @Column(name = "path", length = PATH_LENGTH)
    private String path;

    @Size(max = METHOD_LENGTH)
    @Column(name = "http_method", length = METHOD_LENGTH)
    private String httpMethod;

    @Column(name = "parent_resource_id")
    private Integer parentResourceId;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = Boolean.TRUE;

    @Size(max = DESC_LENGTH)
    @Column(name = "en_description", length = DESC_LENGTH)
    private String enDescription;

    @Size(max = DESC_LENGTH)
    @Column(name = "ar_description", length = DESC_LENGTH)
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
        if (resourceCd != null) {
            resourceCd = resourceCd.trim();
        }
        if (resourceEnNm != null) {
            resourceEnNm = resourceEnNm.trim();
        }
        if (resourceArNm != null) {
            resourceArNm = resourceArNm.trim();
        }
        if (path != null) {
            path = path.trim();
        }
        if (httpMethod != null) {
            httpMethod = httpMethod.trim().toUpperCase(Locale.ROOT);
        }
        if (isActive == null) {
            isActive = Boolean.TRUE;
        }
    }

}
