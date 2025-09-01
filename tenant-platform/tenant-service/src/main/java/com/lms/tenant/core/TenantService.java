package com.lms.tenant.core;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class TenantService {

    private final TenantSettingsPort settingsPort;
    private final NamedParameterJdbcTemplate jdbc;

    public record Tenant(UUID id, String slug, String name, boolean overageEnabled) {}

    public TenantService(TenantSettingsPort settingsPort, NamedParameterJdbcTemplate jdbc) {
        this.settingsPort = settingsPort;
        this.jdbc = jdbc;
    }

    public Tenant createTenant(String slug, String name) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "insert into tenant (tenant_id, tenant_slug, name, status) values (:id,:slug,:name,'ACTIVE')",
                Map.of("id", id, "slug", slug, "name", name));
        // ensure RLS context for integration-key operations
        jdbc.getJdbcOperations().execute("select set_config('app.current_tenant', '" + id + "', true)");
        return new Tenant(id, slug, name, false);
    }

    public Tenant findTenant(UUID id) {
        return jdbc.query(
                "select tenant_id, tenant_slug, name, overage_enabled from tenant where tenant_id = :id",
                Map.of("id", id),
                rs -> rs.next() ? new Tenant(
                        (UUID) rs.getObject("tenant_id"),
                        rs.getString("tenant_slug"),
                        rs.getString("name"),
                        rs.getBoolean("overage_enabled")) : null);
    }

    public void toggleOverage(UUID tenantId, boolean enabled) {
        settingsPort.setOverageEnabled(tenantId, enabled);
    }

    public boolean isOverageEnabled(UUID tenantId) {
        return settingsPort.isOverageEnabled(tenantId);
    }
}
