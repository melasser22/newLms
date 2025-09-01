package com.lms.tenantservice.service;

import com.lms.tenantservice.domain.Tenant;
import com.lms.tenantservice.domain.TenantStatus;
import com.lms.tenantservice.repository.TenantRepository;
import com.lms.tenantservice.web.dto.CreateTenantRequest;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantLifecycleService {

    private final TenantRepository tenantRepository;

    public Tenant createTenant(CreateTenantRequest request) {
        Tenant tenant = Tenant.builder()
                .id(UUID.randomUUID())
                .name(request.name())
                .slug(request.slug())
                .status(TenantStatus.CREATED)
                .locale(request.locale())
                .timezone(request.timezone())
                .domains(request.domains() != null ? new HashSet<>(request.domains()) : new HashSet<>())
                .build();
        return tenantRepository.save(tenant);
    }

    public Tenant updateStatus(UUID id, TenantStatus status) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));
        tenant.setStatus(status);
        return tenantRepository.save(tenant);
    }
}
