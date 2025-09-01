package com.lms.tenantservice.repository;

import com.lms.tenantservice.domain.Tenant;
import com.lms.tenantservice.domain.TenantStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class TenantRepositoryTest {

    @Autowired
    private TenantRepository tenantRepository;

    @Test
    void saveAndRetrieveTenant() {
        Tenant tenant = Tenant.builder()
                .id(UUID.randomUUID())
                .name("Acme")
                .slug("acme")
                .status(TenantStatus.ACTIVE)
                .locale("en_US")
                .timezone("UTC")
                .domains(Set.of("acme.example.com"))
                .build();

        tenantRepository.save(tenant);

        Optional<Tenant> found = tenantRepository.findById(tenant.getId());
        assertTrue(found.isPresent());
        assertEquals("acme", found.get().getSlug());
        assertEquals(TenantStatus.ACTIVE, found.get().getStatus());
        assertEquals(Set.of("acme.example.com"), found.get().getDomains());
    }
}
