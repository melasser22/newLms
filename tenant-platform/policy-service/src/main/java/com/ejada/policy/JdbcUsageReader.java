package com.ejada.policy;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class JdbcUsageReader implements UsageReader {
    private final JdbcTemplate jdbcTemplate;

    public JdbcUsageReader(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public long currentUsage(UUID tenantId, String featureKey, Instant periodStart, Instant periodEnd) {
        // Dummy example query; real implementation would depend on schema
        return jdbcTemplate.queryForObject(
                "select coalesce(sum(delta),0) from usage where tenant_id=? and feature_key=?",
                new Object[]{tenantId, featureKey},
                Long.class
        );
    }
}
