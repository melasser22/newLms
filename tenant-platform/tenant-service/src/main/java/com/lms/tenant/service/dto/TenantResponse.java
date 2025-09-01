package com.lms.tenant.service.dto;

import java.util.UUID;

public record TenantResponse(UUID id, String slug, String name, boolean overageEnabled) {}
