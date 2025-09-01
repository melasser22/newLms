package com.lms.tenantservice.web.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Set;

public record CreateTenantRequest(
        @NotBlank String name,
        @NotBlank String slug,
        Set<@NotBlank String> domains,
        String locale,
        String timezone
) {
}
