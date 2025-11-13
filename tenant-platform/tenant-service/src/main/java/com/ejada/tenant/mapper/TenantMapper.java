package com.ejada.tenant.mapper;

import com.ejada.mapstruct.starter.config.SharedMapstructConfig;
import com.ejada.tenant.dto.TenantCreateReq;
import com.ejada.tenant.dto.TenantRes;
import com.ejada.tenant.dto.TenantUpdateReq;
import com.ejada.tenant.model.Tenant;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.springframework.lang.NonNull;

@Mapper(config = SharedMapstructConfig.class)
public interface TenantMapper {

    // ---------- Create ----------
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", source = "active")
    @Mapping(target = "isDeleted", constant = "false")
    @Mapping(target = "internalTenantId", source = "internalTenantId")
    @Mapping(target = "integrationKeys", ignore = true)
    // DB-managed timestamps
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Tenant toEntity(@NonNull TenantCreateReq req);

    // Post-process defaults
    @AfterMapping
    default void setDefaults(@MappingTarget Tenant e, TenantCreateReq req) {
        if (e.getActive() == null) {
            e.setActive(Boolean.TRUE);
        }
        if (e.getIsDeleted() == null) {
            e.setIsDeleted(Boolean.FALSE);
        }
        if (e.getInternalTenantId() == null && e.getCode() != null) {
            e.setInternalTenantId(generateInternalTenantId(e.getCode()));
        }
    }

    // ---------- Update (PATCH/PUT with IGNORE nulls) ----------
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "integrationKeys", ignore = true)
    @Mapping(target = "internalTenantId", ignore = true)
    void update(@MappingTarget @NonNull Tenant entity, @NonNull TenantUpdateReq req);

    @AfterMapping
    default void applyInternalTenantId(@MappingTarget Tenant entity, TenantUpdateReq req) {
        if (req.internalTenantId() != null) {
            entity.setInternalTenantId(req.internalTenantId());
        }
    }

    // ---------- Response ----------
    @Mapping(target = "isDeleted", source = "isDeleted")
    TenantRes toRes(@NonNull Tenant entity);

    // ---------- Helpers ----------
    static Tenant ref(Integer id) {
        return Tenant.ref(id);
    }

    private static java.util.UUID generateInternalTenantId(String code) {
        if (code == null) {
            return null;
        }
        String key = "tenant:" + code;
        return java.util.UUID.nameUUIDFromBytes(key.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}