package com.lms.tenant.core.adapters.jdbc;

import com.lms.tenant.core.TenantSettingsPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConditionalOnMissingBean(TenantSettingsPort.class)
public class JdbcTenantSettingsPort implements TenantSettingsPort {
    private final NamedParameterJdbcTemplate jdbc;

    public JdbcTenantSettingsPort(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean isOverageEnabled(UUID tenantId) {
        throw new UnsupportedOperationException("JDBC tenant settings lookup not implemented");
    }

    @Override
    public void setOverageEnabled(UUID tenantId, boolean enabled) {
        throw new UnsupportedOperationException("JDBC tenant settings update not implemented");
    }
}
