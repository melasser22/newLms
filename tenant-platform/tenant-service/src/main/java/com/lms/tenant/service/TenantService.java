package com.lms.tenant.service;

import com.common.context.TenantContext;
import com.lms.tenant.persistence.entity.Tenant;
import com.lms.tenant.persistence.entity.TenantIntegrationKey;
import com.lms.tenant.persistence.entity.enums.KeyStatus;
import com.lms.tenant.persistence.entity.enums.TenantStatus;
import com.lms.tenant.persistence.repository.TenantIntegrationKeyRepository;
import com.lms.tenant.persistence.repository.TenantRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class TenantService {
    private final TenantRepository tenantRepository;
    private final TenantIntegrationKeyRepository keyRepository;
    private final TenantSettingsPort settingsPort;

    public TenantService(TenantRepository tenantRepository,
                         TenantIntegrationKeyRepository keyRepository,
                         TenantSettingsPort settingsPort) {
        this.tenantRepository = tenantRepository;
        this.keyRepository = keyRepository;
        this.settingsPort = settingsPort;
    }

    public Tenant createTenant(String slug, String name) {
        Tenant tenant = new Tenant();
        tenant.setId(UUID.randomUUID());
        tenant.setSlug(slug);
        tenant.setName(name);
        tenant.setStatus(TenantStatus.ACTIVE);
        tenantRepository.save(tenant);

        // create default integration key respecting RLS
        TenantContext.set(tenant.getId());
        try {
            TenantIntegrationKey key = new TenantIntegrationKey();
            key.setId(UUID.randomUUID());
            key.setTenant(tenant);
            key.setName("default");
            key.setKey(UUID.randomUUID().toString());
            key.setStatus(KeyStatus.ACTIVE);
            keyRepository.save(key);
        } finally {
            TenantContext.clear();
        }
        return tenant;
    }

    public Tenant getTenant(UUID tenantId) {
        return tenantRepository.findById(tenantId).orElse(null);
    }

    public long getTotalTenantsByStatus(TenantStatus status) {
        return tenantRepository.countByStatus(status);
    }

    public void setOverage(UUID tenantId, boolean enabled) {
        settingsPort.setOverageEnabled(tenantId, enabled);
    }
}
