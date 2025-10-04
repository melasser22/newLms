package com.ejada.tenant.multitenancy;

import com.ejada.starter_core.tenant.TenantDirectory;
import com.ejada.tenant.model.Tenant;
import com.ejada.tenant.repository.TenantRepository;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Database backed implementation of {@link TenantDirectory} for the tenant
 * service. Uses the shared tenant table to resolve tenants by UUID or public
 * slug (code).
 */
@Component
public class JpaTenantDirectory implements TenantDirectory {

    private final TenantRepository repository;

    public JpaTenantDirectory(TenantRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<TenantRecord> findById(UUID tenantId) {
        if (tenantId == null) {
            return Optional.empty();
        }
        return repository.findBySecurityTenantIdAndIsDeletedFalse(tenantId)
                .flatMap(this::mapTenant);
    }

    @Override
    public Optional<TenantRecord> findBySlug(String slug) {
        if (slug == null) {
            return Optional.empty();
        }
        return repository.findByCodeIgnoreCaseAndIsDeletedFalse(slug)
                .flatMap(this::mapTenant);
    }

    private Optional<TenantRecord> mapTenant(Tenant tenant) {
        if (tenant == null || tenant.getSecurityTenantId() == null) {
            return Optional.empty();
        }
        boolean active = tenant.isActive() && !tenant.isDeleted();
        String slug = tenant.getCode() != null ? tenant.getCode().toLowerCase(Locale.ROOT) : null;
        return Optional.of(new TenantRecord(tenant.getSecurityTenantId(), slug, active));
    }
}

