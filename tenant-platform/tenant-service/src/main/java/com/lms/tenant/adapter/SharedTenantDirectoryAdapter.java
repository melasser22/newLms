package com.lms.tenant.adapter;

import com.lms.tenant.core.TenantDirectoryPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@ConditionalOnClass(name = "com.shared.tenant.SharedTenantDirectoryClient")
public class SharedTenantDirectoryAdapter implements TenantDirectoryPort {
    @Override
    public UUID resolveTenantIdBySlugOrDomain(String key) {
        return UUID.nameUUIDFromBytes(("shared-" + key).getBytes());
    }
}
