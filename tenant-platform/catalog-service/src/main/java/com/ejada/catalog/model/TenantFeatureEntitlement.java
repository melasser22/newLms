package com.ejada.catalog.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Table(
        name = "tenant_feature_entitlement",
        uniqueConstraints = @UniqueConstraint(name = "uk_tenant_feature", columnNames = {"tenant_code", "feature_cd"}),
        indexes = @Index(name = "idx_tenant_feature_code", columnList = "tenant_code")
)
@DynamicUpdate
@Getter
@Setter
@NoArgsConstructor
public class TenantFeatureEntitlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tenant_feature_entitlement_id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "tenant_code", length = 64, nullable = false)
    private String tenantCode;

    @Column(name = "feature_cd", length = 128, nullable = false)
    private String featureCode;

    @Column(name = "feature_count")
    private Integer featureCount;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false)
    private OffsetDateTime updatedAt;
}
