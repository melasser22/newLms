package com.lms.tenant.core;

public interface TenantDirectoryPort {
    java.util.UUID resolveTenantIdBySlugOrDomain(String key);
}
