package com.lms.tenant.api.mapper;

import com.lms.tenant.api.dto.CreateTenantRequest;
import com.lms.tenant.api.dto.TenantResponse;
import com.lms.tenant.domain.Tenant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface TenantApiMapper {

    TenantApiMapper INSTANCE = Mappers.getMapper(TenantApiMapper.class);

    /**
     * Maps a CreateTenantRequest DTO to a Tenant domain object.
     * Note: Fields not present in the DTO (like id, status, etc.) will be null
     * and should be set by the application service during creation.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "tierId", ignore = true)
    @Mapping(target = "timezone", ignore = true)
    @Mapping(target = "locale", ignore = true)
    @Mapping(target = "domains", ignore = true)
    @Mapping(target = "overageEnabled", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Tenant toDomain(CreateTenantRequest dto);

    /**
     * Maps a Tenant domain object to a TenantResponse DTO.
     */
    TenantResponse toResponse(Tenant domain);
}
