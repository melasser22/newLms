package com.lms.tenantservice.service;

import com.lms.tenantservice.domain.Tenant;
import com.lms.tenantservice.domain.TenantStatus;
import com.lms.tenantservice.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(TenantLifecycleService.class)
class TenantLifecycleServiceTest {

    @Autowired
    private TenantLifecycleService service;

    @Autowired
    private TenantRepository repository;

    @Test
    void getTenantReturnsPersistedTenant() {
        Tenant tenant = Tenant.builder()
                .id(UUID.randomUUID())
                .name("Acme")
                .slug("acme")
                .status(TenantStatus.ACTIVE)
                .domains(Set.of())
                .build();
        repository.save(tenant);

        Tenant found = service.getTenant(tenant.getId());
        assertEquals("acme", found.getSlug());
    }

    @Test
    void listTenantsReturnsAllTenants() {
        Tenant t1 = Tenant.builder()
                .id(UUID.randomUUID())
                .name("Tenant1")
                .slug("tenant1")
                .status(TenantStatus.CREATED)
                .domains(Set.of())
                .build();
        Tenant t2 = Tenant.builder()
                .id(UUID.randomUUID())
                .name("Tenant2")
                .slug("tenant2")
                .status(TenantStatus.CREATED)
                .domains(Set.of())
                .build();
        repository.save(t1);
        repository.save(t2);

        assertEquals(2, service.listTenants().size());
    }
}
