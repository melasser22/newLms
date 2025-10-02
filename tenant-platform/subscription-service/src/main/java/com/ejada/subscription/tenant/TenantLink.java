package com.ejada.subscription.tenant;

import java.util.UUID;

public record TenantLink(String tenantCode, String tenantName, UUID securityTenantId) {}
