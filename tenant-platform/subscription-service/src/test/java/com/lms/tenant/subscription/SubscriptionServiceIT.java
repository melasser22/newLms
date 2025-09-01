package com.lms.tenant.subscription;

import com.lms.tenant.subscription.service.SubscriptionService;
import com.lms.tenant.subscription.repo.TenantSubscriptionRepository;
import com.shared.subscription.api.SubscriptionDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import org.springframework.dao.DataIntegrityViolationException;

@SpringBootTest
@Testcontainers
@ExtendWith(SpringExtension.class)
class SubscriptionServiceIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    SubscriptionService service;

    @Autowired
    TenantSubscriptionRepository repository;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    @Transactional
    void enforcesSingleActiveSubscriptionPerTenant() {
        UUID tenantId = UUID.randomUUID();
        jdbc.execute("select set_config('app.current_tenant', '" + tenantId + "', true)");
        repository.saveAndFlush(new com.lms.tenant.subscription.domain.TenantSubscription(UUID.randomUUID(), tenantId, com.lms.tenant.subscription.domain.SubscriptionStatus.TRIAL, true));
        assertThatThrownBy(() -> {
            repository.saveAndFlush(new com.lms.tenant.subscription.domain.TenantSubscription(UUID.randomUUID(), tenantId, com.lms.tenant.subscription.domain.SubscriptionStatus.ACTIVE, true));
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void findsActiveSubscription() {
        UUID tenantId = UUID.randomUUID();
        jdbc.execute("select set_config('app.current_tenant', '" + tenantId + "', true)");
        SubscriptionDto dto = service.startTrial(tenantId);
        assertThat(service.findActiveSubscription(tenantId)).isPresent();
        service.cancel(tenantId, dto.id());
        assertThat(service.findActiveSubscription(tenantId)).isEmpty();
    }
}
