package com.lms.tenant.web;

import java.util.UUID;

public record TenantResponse(UUID id, String slug, String name, boolean overageEnabled) {}
