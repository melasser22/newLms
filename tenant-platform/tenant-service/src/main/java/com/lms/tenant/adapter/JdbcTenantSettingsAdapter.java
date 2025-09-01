package com.lms.tenant.adapter;

import com.lms.tenant.core.TenantSettingsPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.UUID;

@Repository
@ConditionalOnMissingBean(TenantSettingsPort.class)
public class JdbcTenantSettingsAdapter implements TenantSettingsPort {
    private final NamedParameterJdbcTemplate jdbc;

    public JdbcTenantSettingsAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean isOverageEnabled(UUID tenantId) {
        Boolean result = jdbc.queryForObject(
                "select overage_enabled from tenant where tenant_id = :id",
                Map.of("id", tenantId),
                Boolean.class);
        return Boolean.TRUE.equals(result);
    }

    @Override
    public void setOverageEnabled(UUID tenantId, boolean enabled) {
        int updated = jdbc.update(
                "update tenant set overage_enabled = :enabled, updated_at = now() where tenant_id = :id",
                Map.of("id", tenantId, "enabled", enabled));
        if (updated == 0) {
            jdbc.update(
                    "insert into tenant (tenant_id, overage_enabled) values (:id, :enabled)",
                    Map.of("id", tenantId, "enabled", enabled));
        }
    }
}
