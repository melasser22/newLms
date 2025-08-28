package com.lms.setup.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Resource() {}

    @PrePersist @PreUpdate
    private void normalize() {
        if (resourceCd != null) resourceCd = resourceCd.trim();
        if (resourceEnNm != null) resourceEnNm = resourceEnNm.trim();
        if (resourceArNm != null) resourceArNm = resourceArNm.trim();
        if (path != null) path = path.trim();
        if (httpMethod != null) httpMethod = httpMethod.trim().toUpperCase();
        if (isActive == null) isActive = Boolean.TRUE;
    }

    // getters/setters
    public Integer getResourceId() { return resourceId; }
    public void setResourceId(Integer resourceId) { this.resourceId = resourceId; }

    public String getResourceCd() { return resourceCd; }
    public void setResourceCd(String resourceCd) { this.resourceCd = resourceCd; }

    public String getResourceEnNm() { return resourceEnNm; }
    public void setResourceEnNm(String resourceEnNm) { this.resourceEnNm = resourceEnNm; }

    public String getResourceArNm() { return resourceArNm; }
    public void setResourceArNm(String resourceArNm) { this.resourceArNm = resourceArNm; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getHttpMethod() { return httpMethod; }
    public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }

    public Integer getParentResourceId() { return parentResourceId; }
    public void setParentResourceId(Integer parentResourceId) { this.parentResourceId = parentResourceId; }

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
        if (!(o instanceof Resource that)) return false;
        return Objects.equals(resourceId, that.resourceId);
    }
    @Override public int hashCode() { return Objects.hash(resourceId); }
    @Override public String toString() {
        return "Resource{resourceId=" + resourceId + ", resourceCd='" + resourceCd + "'}";
    }
}
