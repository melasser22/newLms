package com.ejada.tenant.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;

@Schema(name = "TenantIntegrationKeyRes")
public record TenantIntegrationKeyRes(
        Long tikId,
        Integer tenantId,
        String keyId,
        String label,
        List<String> scopes,
        TikStatus status,
        OffsetDateTime validFrom,
        OffsetDateTime expiresAt,
        OffsetDateTime lastUsedAt,
        Long useCount,
        Integer dailyQuota,
        String meta,
        Boolean isDeleted,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public TenantIntegrationKeyRes {
        scopes = scopes == null ? List.of() : List.copyOf(scopes);
    }

    @Override
    public List<String> scopes() {
        return List.copyOf(scopes);
    }
}