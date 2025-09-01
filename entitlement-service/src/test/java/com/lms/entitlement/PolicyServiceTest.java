package com.lms.entitlement;

import com.lms.entitlement.entity.*;
import com.lms.entitlement.repository.*;
import com.lms.entitlement.service.PolicyService;
import com.lms.entitlement.service.dto.OverageRecordDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
public class PolicyServiceTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired private TenantRepository tenantRepository;
    @Autowired private FeatureRepository featureRepository;
    @Autowired private ProductTierRepository tierRepository;
    @Autowired private TierFeatureLimitRepository limitRepository;
    @Autowired private TenantSubscriptionRepository subscriptionRepository;
    @Autowired private PolicyService policyService;

    private Tenant tenant;
    private Feature feature;
    private TenantSubscription subscription;

    @BeforeEach
    void setup() {
        tenantRepository.deleteAll();
        featureRepository.deleteAll();
        tierRepository.deleteAll();
        limitRepository.deleteAll();
        subscriptionRepository.deleteAll();

        tenant = new Tenant();
        tenant.setId(UUID.randomUUID());
        tenant.setName("T1");
        tenant.setOverageEnabled(false);
        tenantRepository.save(tenant);

        feature = new Feature();
        feature.setKey("feat");
        feature.setName("Feature");
        featureRepository.save(feature);

        ProductTier tier = new ProductTier();
        tier.setId(UUID.randomUUID());
        tier.setName("Basic");
        tierRepository.save(tier);

        TierFeatureLimit limit = new TierFeatureLimit();
        limit.setTier(tier);
        limit.setFeature(feature);
        limit.setFeatureLimit(10L);
        limit.setAllowOverage(true);
        limit.setOverageUnitPriceMinor(100L);
        limit.setOverageCurrency("USD");
        limitRepository.save(limit);

        subscription = new TenantSubscription();
        subscription.setId(UUID.randomUUID());
        subscription.setTenant(tenant);
        subscription.setTier(tier);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setPeriodStart(Instant.now().minusSeconds(60));
        subscription.setPeriodEnd(Instant.now().plusSeconds(60));
        subscriptionRepository.save(subscription);
    }

    @Test
    void withinLimitNoOverage() {
        Optional<OverageRecordDto> record = policyService.consumeOrOverage(
                tenant.getId(), feature.getKey(), 5L,
                subscription.getPeriodStart(), subscription.getPeriodEnd(), () -> 0L, "idem1");
        assertTrue(record.isEmpty());
    }

    @Test
    void overLimitBlockedWhenTenantNotEnabled() {
        assertThrows(IllegalStateException.class, () ->
                policyService.consumeOrOverage(
                        tenant.getId(), feature.getKey(), 15L,
                        subscription.getPeriodStart(), subscription.getPeriodEnd(), () -> 0L, "idem2"));
    }

    @Test
    void overLimitAllowedAndIdempotent() {
        tenant.setOverageEnabled(true);
        tenantRepository.save(tenant);

        Optional<OverageRecordDto> record = policyService.consumeOrOverage(
                tenant.getId(), feature.getKey(), 15L,
                subscription.getPeriodStart(), subscription.getPeriodEnd(), () -> 0L, "idem3");
        assertTrue(record.isPresent());
        OverageRecordDto first = record.get();

        Optional<OverageRecordDto> second = policyService.consumeOrOverage(
                tenant.getId(), feature.getKey(), 15L,
                subscription.getPeriodStart(), subscription.getPeriodEnd(), () -> 0L, "idem3");
        assertTrue(second.isPresent());
        assertEquals(first.id(), second.get().id());
    }
}
