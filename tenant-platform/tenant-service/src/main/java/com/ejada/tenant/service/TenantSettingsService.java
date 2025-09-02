package com.ejada.tenant.service;

import com.ejada.tenant.persistence.repository.TenantRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class TenantSettingsService implements TenantSettingsPort {
    private final TenantRepository tenantRepository;

    public TenantSettingsService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Override
    public boolean isOverageEnabled(UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .map(com.ejada.tenant.persistence.entity.Tenant::isOverageEnabled)
                .orElse(false);
    }

    @Override
    public void setOverageEnabled(UUID tenantId, boolean enabled) {
        tenantRepository.findById(tenantId).ifPresent(tenant -> {
            tenant.setOverageEnabled(enabled);
            tenantRepository.save(tenant);
        });
    }
}
