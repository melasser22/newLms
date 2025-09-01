package com.lms.catalog.persistence.repo;

import com.lms.catalog.persistence.entity.TenantFeatureOverrideEntity;
import com.lms.catalog.persistence.entity.TenantFeatureOverrideId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface TenantFeatureOverrideRepository extends JpaRepository<TenantFeatureOverrideEntity, TenantFeatureOverrideId> {

    Optional<TenantFeatureOverrideEntity> findByIdTenantIdAndIdFeatureKey(UUID tenantId, String featureKey);

    @Modifying
    @Query(value = """
            insert into tenant_feature_override(tenant_id, feature_key, enabled, limit_value, allow_overage_override, overage_unit_price_minor_override, overage_currency_override)
            values (:tenantId, :featureKey, :enabled, :limit, :allowOverage, :overagePrice, :overageCurrency)
            on conflict (tenant_id, feature_key) do update set
                enabled = excluded.enabled,
                limit_value = excluded.limit_value,
                allow_overage_override = excluded.allow_overage_override,
                overage_unit_price_minor_override = excluded.overage_unit_price_minor_override,
                overage_currency_override = excluded.overage_currency_override
            """, nativeQuery = true)
    void upsert(@Param("tenantId") UUID tenantId,
                @Param("featureKey") String featureKey,
                @Param("enabled") Boolean enabled,
                @Param("limit") Long limit,
                @Param("allowOverage") Boolean allowOverage,
                @Param("overagePrice") Long overagePrice,
                @Param("overageCurrency") String overageCurrency);
}
