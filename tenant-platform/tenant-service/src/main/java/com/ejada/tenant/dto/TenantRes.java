package com.ejada.tenant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(name = "TenantRes")
public record TenantRes(
        Integer id,
        String code,
        String name,
        String contactEmail,
        String contactPhone,
        String logoUrl,
        Boolean active,
        UUID internalTenantId,
        Boolean isDeleted,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) { }