package com.ejada.tenant.mapper;

import com.ejada.tenant.dto.*;
import com.ejada.tenant.model.Tenant;
import com.ejada.tenant.model.TenantIntegrationKey;
import com.ejada.tenant.model.TenantIntegrationKey.Status;
import org.mapstruct.*;

import java.time.OffsetDateTime;

@Mapper(componentModel = "spring",
        imports = { Tenant.class, TenantIntegrationKey.class, OffsetDateTime.class },
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface TenantIntegrationKeyMapper {

    // ---------- Create ----------
    @Mapping(target = "tikId", ignore = true)
    @Mapping(target = "tenant", expression = "java(Tenant.ref(req.tenantId()))")
    @Mapping(target = "keyId", source = "keyId")
    // Hash the secret in service layer; mapper just copies field if provided
    @Mapping(target = "keySecret", ignore = true)
    @Mapping(target = "label", source = "label")
    @Mapping(target = "scopes", source = "scopes", qualifiedByName = "toArray")
    @Mapping(target = "status", source = "status", qualifiedByName = "toEntityStatus")
    @Mapping(target = "validFrom", source = "validFrom")
    @Mapping(target = "expiresAt", source = "expiresAt")
    @Mapping(target = "lastUsedAt", ignore = true)
    @Mapping(target = "useCount", constant = "0L")
    @Mapping(target = "dailyQuota", source = "dailyQuota")
    @Mapping(target = "meta", source = "meta")
    @Mapping(target = "isDeleted", constant = "false")
    // DB-managed timestamps
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    TenantIntegrationKey toEntity(TenantIntegrationKeyCreateReq req);

    @AfterMapping
    default void setDefaults(@MappingTarget TenantIntegrationKey e, TenantIntegrationKeyCreateReq req) {
        if (e.getValidFrom() == null) e.setValidFrom(OffsetDateTime.now());
        if (e.getStatus() == null) e.setStatus(Status.ACTIVE);
        if (e.getUseCount() == null) e.setUseCount(0L);
        if (e.getIsDeleted() == null) e.setIsDeleted(Boolean.FALSE);
    }

    // ---------- Update (PATCH/PUT with IGNORE nulls) ----------
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "scopes", source = "scopes", qualifiedByName = "toArray")
    @Mapping(target = "status", source = "status", qualifiedByName = "toEntityStatus")
    void update(@MappingTarget TenantIntegrationKey entity, TenantIntegrationKeyUpdateReq req);

    // ---------- Response ----------
    @Mapping(target = "tenantId", source = "tenant.id")
    @Mapping(target = "scopes", source = "scopes", qualifiedByName = "toList")
    @Mapping(target = "status", source = "status", qualifiedByName = "toDtoStatus")
    TenantIntegrationKeyRes toRes(TenantIntegrationKey entity);

    // ---------- Enum & collection converters ----------
    @Named("toEntityStatus")
    default Status toEntityStatus(TikStatus s) {
        if (s == null) return null;
        return switch (s) {
            case ACTIVE -> Status.ACTIVE;
            case SUSPENDED -> Status.SUSPENDED;
            case REVOKED -> Status.REVOKED;
            case EXPIRED -> Status.EXPIRED;
        };
    }

    @Named("toDtoStatus")
    default TikStatus toDtoStatus(Status s) {
        if (s == null) return null;
        return switch (s) {
            case ACTIVE -> TikStatus.ACTIVE;
            case SUSPENDED -> TikStatus.SUSPENDED;
            case REVOKED -> TikStatus.REVOKED;
            case EXPIRED -> TikStatus.EXPIRED;
        };
    }

    @Named("toArray")
    default String[] toArray(java.util.List<String> list) {
        return (list == null) ? null : list.stream().toArray(String[]::new);
    }

    @Named("toList")
    default java.util.List<String> toList(String[] arr) {
        return (arr == null) ? null : java.util.Arrays.asList(arr);
    }
}