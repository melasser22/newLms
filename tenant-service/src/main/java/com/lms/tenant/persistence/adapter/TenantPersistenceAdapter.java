package com.lms.tenant.persistence.adapter;

import com.lms.tenant.application.port.out.TenantRepositoryPort;
import com.lms.tenant.domain.Tenant;
import com.lms.tenant.persistence.entity.TenantEntity;
import com.lms.tenant.persistence.mapper.TenantPersistenceMapper;
import com.lms.tenant.persistence.repository.TenantJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
@Transactional(readOnly = true)
public class TenantPersistenceAdapter implements TenantRepositoryPort {

    private final TenantJpaRepository tenantJpaRepository;
    private final TenantPersistenceMapper mapper;

    public TenantPersistenceAdapter(TenantJpaRepository tenantJpaRepository, TenantPersistenceMapper mapper) {
        this.tenantJpaRepository = tenantJpaRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public Tenant save(Tenant tenant) {
        TenantEntity entity = mapper.toEntity(tenant);
        TenantEntity savedEntity = tenantJpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Tenant> findById(UUID id) {
        return tenantJpaRepository.findById(id)
            .map(mapper::toDomain);
    }

    @Override
    public Optional<Tenant> findBySlug(String slug) {
        return tenantJpaRepository.findBySlug(slug)
            .map(mapper::toDomain);
    }
}
