package com.lms.catalog.core;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class EffectiveFeatureResolverIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    FeaturePolicyPort port;

    @Autowired
    EntityManager em;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void mergesTenantOverride() {
        UUID tenantId = UUID.randomUUID();
        em.createNativeQuery("insert into feature(feature_key, description) values ('feat','d')").executeUpdate();
        em.createNativeQuery("insert into product_tier(tier_id, name, description, active, is_default, billing_external_ids) values ('tier','t','d', true, true, '{}'::jsonb)").executeUpdate();
        em.createNativeQuery("insert into tier_feature_limit(tier_id, feature_key, enabled, limit_value, allow_overage, overage_unit_price_minor, overage_currency) values ('tier','feat', true,100,false,null,null)").executeUpdate();

        port.upsertOverride(tenantId, "feat", new FeaturePolicyPort.FeatureOverride(null, 200L, true, 5L, "USD"));

        var result = port.effective("tier", tenantId, "feat");
        assertThat(result.limit()).isEqualTo(200L);
        assertThat(result.allowOverage()).isTrue();
        assertThat(result.overageUnitPriceMinor()).isEqualTo(5L);
    }
}
