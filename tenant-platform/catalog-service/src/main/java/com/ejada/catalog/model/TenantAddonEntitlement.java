package com.ejada.catalog.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Table(
        name = "tenant_addon_entitlement",
        uniqueConstraints = @UniqueConstraint(name = "uk_tenant_addon", columnNames = {"tenant_code", "addon_cd"}),
        indexes = @Index(name = "idx_tenant_addon_code", columnList = "tenant_code")
)
@DynamicUpdate
@Getter
@Setter
@NoArgsConstructor
public class TenantAddonEntitlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tenant_addon_entitlement_id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "tenant_code", length = 64, nullable = false)
    private String tenantCode;

    @Column(name = "addon_cd", length = 128, nullable = false)
    private String addonCode;

    @Column(name = "product_additional_service_id")
    private Long productAdditionalServiceId;

    @Column(name = "service_name_en", length = 256)
    private String serviceNameEn;

    @Column(name = "service_name_ar", length = 256)
    private String serviceNameAr;

    @Column(name = "service_price", precision = 18, scale = 4)
    private BigDecimal servicePrice;

    @Column(name = "total_amount", precision = 18, scale = 4)
    private BigDecimal totalAmount;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "is_countable")
    private Boolean countable;

    @Column(name = "requested_count")
    private Long requestedCount;

    @Column(name = "payment_type_cd", length = 32)
    private String paymentTypeCd;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false)
    private OffsetDateTime updatedAt;
}
