package com.lms.tenant.persistence.adapter;

import com.lms.tenant.application.port.out.IntegrationKeyRepositoryPort;
import com.lms.tenant.domain.IntegrationKey;
import com.lms.tenant.persistence.entity.TenantIntegrationKeyEntity;
import com.lms.tenant.persistence.mapper.IntegrationKeyPersistenceMapper;
import com.lms.tenant.persistence.repository.TenantIntegrationKeyJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Transactional(readOnly = true)
public class IntegrationKeyPersistenceAdapter implements IntegrationKeyRepositoryPort {

    private final TenantIntegrationKeyJpaRepository repository;
    private final IntegrationKeyPersistenceMapper mapper;

    public IntegrationKeyPersistenceAdapter(TenantIntegrationKeyJpaRepository repository, IntegrationKeyPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public IntegrationKey save(IntegrationKey integrationKey) {
        TenantIntegrationKeyEntity entity = mapper.toEntity(integrationKey);
        TenantIntegrationKeyEntity savedEntity = repository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public List<IntegrationKey> findByTenantId(UUID tenantId) {
        return repository.findByTenantId(tenantId).stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public Optional<IntegrationKey> findByTenantIdAndKeyPrefix(UUID tenantId, String keyPrefix) {
        return repository.findByTenantIdAndKeyPrefix(tenantId, keyPrefix)
            .map(mapper::toDomain);
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}
