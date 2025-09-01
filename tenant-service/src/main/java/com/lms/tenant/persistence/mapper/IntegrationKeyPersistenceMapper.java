package com.lms.tenant.persistence.mapper;

import com.lms.tenant.domain.IntegrationKey;
import com.lms.tenant.persistence.entity.TenantIntegrationKeyEntity;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface IntegrationKeyPersistenceMapper {

    IntegrationKeyPersistenceMapper INSTANCE = Mappers.getMapper(IntegrationKeyPersistenceMapper.class);

    /**
     * Maps an IntegrationKey domain object to a TenantIntegrationKeyEntity.
     *
     * @param integrationKey The domain object.
     * @return The JPA entity.
     */
    TenantIntegrationKeyEntity toEntity(IntegrationKey integrationKey);

    /**
     * Maps a TenantIntegrationKeyEntity to an IntegrationKey domain object.
     *
     * @param entity The JPA entity.
     * @return The domain object.
     */
    IntegrationKey toDomain(TenantIntegrationKeyEntity entity);
}
