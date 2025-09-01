package com.lms.tenant.application.service;

import com.lms.tenant.application.port.in.TenantUseCase;
import com.lms.tenant.application.port.out.TenantRepositoryPort;
import com.lms.tenant.domain.Tenant;
import com.lms.tenant.domain.TenantStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class TenantService implements TenantUseCase {

    private final TenantRepositoryPort tenantRepository;

    public TenantService(TenantRepositoryPort tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Override
    @Transactional
    public Tenant createTenant(Tenant tenant) {
        // Basic validation
        if (tenantRepository.findBySlug(tenant.slug()).isPresent()) {
            throw new IllegalArgumentException("Tenant with slug '" + tenant.slug() + "' already exists.");
        }

        // Enrich the domain object with system-generated values
        Tenant newTenant = new Tenant(
            UUID.randomUUID(),
            tenant.slug(),
            tenant.name(),
            TenantStatus.ACTIVE,
            null, // tierId can be set later via subscription service
            "UTC",
            "en",
            List.of(),
            false,
            Instant.now(),
            Instant.now()
        );

        return tenantRepository.save(newTenant);
    }

    @Override
    public Optional<Tenant> findTenantById(UUID id) {
        return tenantRepository.findById(id);
    }

    @Override
    public Optional<Tenant> findTenantBySlug(String slug) {
        return tenantRepository.findBySlug(slug);
    }

    @Override
    @Transactional
    public Tenant setOveragePolicy(UUID tenantId, boolean isEnabled) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found with ID: " + tenantId));

        Tenant updatedTenant = new Tenant(
            tenant.id(),
            tenant.slug(),
            tenant.name(),
            tenant.status(),
            tenant.tierId(),
            tenant.timezone(),
            tenant.locale(),
            tenant.domains(),
            isEnabled, // The only changed field
            tenant.createdAt(),
            Instant.now() // updated at
        );

        return tenantRepository.save(updatedTenant);
    }
}
