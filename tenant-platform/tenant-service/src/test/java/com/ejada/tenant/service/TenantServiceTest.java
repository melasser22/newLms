package com.ejada.tenant.service;

import com.ejada.tenant.persistence.entity.Tenant;
import com.ejada.tenant.persistence.entity.enums.TenantStatus;
import com.ejada.tenant.persistence.repository.TenantIntegrationKeyRepository;
import com.ejada.tenant.persistence.repository.TenantRepository;
import com.ejada.tenant.service.TenantSettingsPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.ApplicationEventPublisher;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TenantServiceTest {

    @Autowired
    TenantRepository tenantRepository;

    @Autowired
    TenantIntegrationKeyRepository keyRepository;

    TenantService tenantService;

    @BeforeEach
    void setUp() {
        keyRepository.deleteAll();
        tenantRepository.deleteAll();
        tenantService = new TenantService(
                tenantRepository,
                keyRepository,
                Mockito.mock(TenantSettingsPort.class),
                Mockito.mock(ApplicationEventPublisher.class)
        );
    }

    @Test
    void getTotalTenantsByStatusReturnsCount() {
        Tenant t1 = new Tenant();
        t1.setId(UUID.randomUUID());
        t1.setSlug("foo");
        t1.setName("Foo");
        t1.setStatus(TenantStatus.ACTIVE);
        tenantRepository.save(t1);

        Tenant t2 = new Tenant();
        t2.setId(UUID.randomUUID());
        t2.setSlug("bar");
        t2.setName("Bar");
        t2.setStatus(TenantStatus.ACTIVE);
        tenantRepository.save(t2);

        long total = tenantService.getTotalTenantsByStatus(TenantStatus.ACTIVE);
        assertThat(total).isEqualTo(2);
    }

    @Test
    void getTotalTenantsByStatusReturnsZeroWhenNone() {
        long total = tenantService.getTotalTenantsByStatus(TenantStatus.INACTIVE);
        assertThat(total).isZero();
    }
}
