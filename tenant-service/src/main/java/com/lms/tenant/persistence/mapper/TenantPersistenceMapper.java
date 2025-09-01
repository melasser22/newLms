package com.lms.tenant.persistence.mapper;

import com.lms.tenant.domain.Tenant;
import com.lms.tenant.persistence.entity.TenantEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface TenantPersistenceMapper {

    TenantPersistenceMapper INSTANCE = Mappers.getMapper(TenantPersistenceMapper.class);

    /**
     * Maps a Tenant domain object to a TenantEntity JPA entity.
     *
     * @param tenant The domain object.
     * @return The JPA entity.
     */
    @Mapping(target = "id", source = "id")
    TenantEntity toEntity(Tenant tenant);

    /**
     * Maps a TenantEntity JPA entity to a Tenant domain object.
     *
     * @param entity The JPA entity.
     * @return The domain object.
     */
    @Mapping(target = "id", source = "id")
    Tenant toDomain(TenantEntity entity);
}
