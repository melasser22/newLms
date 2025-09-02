package com.ejada.tenant.persistence;

import com.ejada.tenant.persistence.entity.Tenant;
import com.ejada.tenant.persistence.entity.enums.TenantStatus;
import com.ejada.tenant.persistence.repository.TenantRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class TenantRepositoryIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TenantRepository tenantRepository;

    @Test
    void persistTenant() {
        Tenant tenant = new Tenant();
        tenant.setId(UUID.randomUUID());
        tenant.setStatus(TenantStatus.ACTIVE);
        tenant.setOverageEnabled(true);
        tenant.setSlug("test-" + tenant.getId());
        tenant.setName("Test Tenant");

        tenantRepository.save(tenant);

        assertThat(tenantRepository.findById(tenant.getId())).isPresent();
    }
}

