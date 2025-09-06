package com.ejada.tenant.mapper;

import com.ejada.tenant.dto.*;
import com.ejada.tenant.model.Tenant;
import org.mapstruct.*;

import java.time.OffsetDateTime;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface TenantMapper {

    // ---------- Create ----------
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", source = "active")
    @Mapping(target = "isDeleted", constant = "false")
    // DB-managed timestamps
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Tenant toEntity(TenantCreateReq req);

    // Post-process defaults
    @AfterMapping
    default void setDefaults(@MappingTarget Tenant e, TenantCreateReq req) {
        if (e.getActive() == null) e.setActive(Boolean.TRUE);
        if (e.getIsDeleted() == null) e.setIsDeleted(Boolean.FALSE);
    }

    // ---------- Update (PATCH/PUT with IGNORE nulls) ----------
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void update(@MappingTarget Tenant entity, TenantUpdateReq req);

    // ---------- Response ----------
    TenantRes toRes(Tenant entity);

    // ---------- Helpers ----------
    static Tenant ref(Integer id) { return Tenant.ref(id); }
}