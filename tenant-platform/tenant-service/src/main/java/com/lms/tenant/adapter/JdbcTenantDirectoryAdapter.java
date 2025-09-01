package com.lms.tenant.adapter;

import com.lms.tenant.core.TenantDirectoryPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.UUID;

@Repository
@ConditionalOnMissingBean(TenantDirectoryPort.class)
public class JdbcTenantDirectoryAdapter implements TenantDirectoryPort {
    private final NamedParameterJdbcTemplate jdbc;

    public JdbcTenantDirectoryAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public UUID resolveTenantIdBySlugOrDomain(String key) {
        return jdbc.query(
                "select tenant_id from tenant where tenant_slug = :key or :key = any(domains)",
                Map.of("key", key),
                rs -> rs.next() ? UUID.fromString(rs.getString(1)) : null
        );
    }
}
