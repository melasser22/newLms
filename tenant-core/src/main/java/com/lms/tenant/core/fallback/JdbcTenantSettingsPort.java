package com.lms.tenant.core.fallback;

import com.lms.tenant.core.port.TenantSettingsPort;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.Map;
import java.util.UUID;

class JdbcTenantSettingsPort implements TenantSettingsPort {

    private final NamedParameterJdbcTemplate template;

    JdbcTenantSettingsPort(NamedParameterJdbcTemplate template) {
        this.template = template;
    }

    @Override
    public boolean isOverageEnabled(UUID tenantId) {
        String sql = "select overage_enabled from tenant where tenant_id=:t";
        Boolean enabled = template.queryForObject(sql, Map.of("t", tenantId), Boolean.class);
        return Boolean.TRUE.equals(enabled);
    }

    @Override
    public void setOverageEnabled(UUID tenantId, boolean enabled) {
        String sql = "update tenant set overage_enabled=:e where tenant_id=:t";
        template.update(sql, Map.of("e", enabled, "t", tenantId));
    }
}
