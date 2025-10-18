package com.ejada.tenant.mapper;

import com.ejada.mapstruct.starter.config.SharedMapstructConfig;
import com.ejada.tenant.dto.TenantIntegrationKeyCreateReq;
import com.ejada.tenant.dto.TenantIntegrationKeyRes;
import com.ejada.tenant.dto.TenantIntegrationKeyUpdateReq;
import com.ejada.tenant.dto.TikStatus;
import com.ejada.tenant.model.Tenant;
import com.ejada.tenant.model.TenantIntegrationKey;
import com.ejada.tenant.model.TenantIntegrationKey.Status;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.springframework.lang.NonNull;

import java.time.OffsetDateTime;

@Mapper(
    config = SharedMapstructConfig.class,
    imports = {Tenant.class, TenantIntegrationKey.class, OffsetDateTime.class}
)
public interface TenantIntegrationKeyMapper {

    // ---------- Create ----------
    @BeanMapping(ignoreUnmappedSourceProperties = {"tenantId", "plainSecret"})
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
    @Mapping(target = "createdBy", source = "createdBy")
    @Mapping(target = "secretLastRotatedAt", ignore = true)
    @Mapping(target = "secretLastRotatedBy", ignore = true)
    // DB-managed timestamps
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    TenantIntegrationKey toEntity(@NonNull TenantIntegrationKeyCreateReq req);

    @AfterMapping
    default void setDefaults(@MappingTarget TenantIntegrationKey e, TenantIntegrationKeyCreateReq req) {
        if (e.getValidFrom() == null) {
            e.setValidFrom(OffsetDateTime.now());
        }
        if (e.getStatus() == null) {
            e.setStatus(Status.ACTIVE);
        }
        if (e.getUseCount() == null) {
            e.setUseCount(0L);
        }
        if (e.getIsDeleted() == null) {
            e.setIsDeleted(Boolean.FALSE);
        }
    }

    // ---------- Update (PATCH/PUT with IGNORE nulls) ----------
    @BeanMapping(
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
            ignoreUnmappedSourceProperties = {"newPlainSecret", "rotatedBy"})
    @Mapping(target = "tikId", ignore = true)
    @Mapping(target = "tenant", ignore = true)
    @Mapping(target = "keyId", ignore = true)
    @Mapping(target = "keySecret", ignore = true)
    @Mapping(target = "lastUsedAt", ignore = true)
    @Mapping(target = "useCount", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "secretLastRotatedAt", ignore = true)
    @Mapping(target = "secretLastRotatedBy", ignore = true)
    @Mapping(target = "scopes", source = "scopes", qualifiedByName = "toArray")
    @Mapping(target = "status", source = "status", qualifiedByName = "toEntityStatus")
    void update(@MappingTarget @NonNull TenantIntegrationKey entity, @NonNull TenantIntegrationKeyUpdateReq req);

    // ---------- Response ----------
    default TenantIntegrationKeyRes toRes(@NonNull TenantIntegrationKey entity) {
        Integer tenantId = null;
        Tenant tenant = entity.getTenant();
        if (tenant != null) {
            tenantId = tenant.getId();
        }

        return new TenantIntegrationKeyRes(
                entity.getTikId(),
                tenantId,
                entity.getKeyId(),
                entity.getLabel(),
                toList(entity.getScopes()),
                toDtoStatus(entity.getStatus()),
                entity.getValidFrom(),
                entity.getExpiresAt(),
                entity.getLastUsedAt(),
                entity.getUseCount(),
                entity.getDailyQuota(),
                entity.getMeta(),
                entity.getIsDeleted(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getCreatedBy(),
                entity.getSecretLastRotatedAt(),
                entity.getSecretLastRotatedBy(),
                null
        );
    }

    // ---------- Enum & collection converters ----------
    @Named("toEntityStatus")
    default Status toEntityStatus(TikStatus s) {
        if (s == null) {
            return null;
        }
        return switch (s) {
            case ACTIVE -> Status.ACTIVE;
            case SUSPENDED -> Status.SUSPENDED;
            case REVOKED -> Status.REVOKED;
            case EXPIRED -> Status.EXPIRED;
        };
    }

    @Named("toDtoStatus")
    default TikStatus toDtoStatus(Status s) {
        if (s == null) {
            return null;
        }
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