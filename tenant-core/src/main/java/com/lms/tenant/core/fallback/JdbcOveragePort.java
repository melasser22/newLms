package com.lms.tenant.core.fallback;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.tenant.core.port.OveragePort;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

class JdbcOveragePort implements OveragePort {

    private final NamedParameterJdbcTemplate template;
    private final ObjectMapper mapper;

    JdbcOveragePort(NamedParameterJdbcTemplate template, ObjectMapper mapper) {
        this.template = template;
        this.mapper = mapper;
    }

    @Override
    public UUID recordOverage(UUID tenantId, UUID subscriptionId, String featureKey, long quantity, Long unitPriceMinor,
                              String currency, Instant periodStart, Instant periodEnd, String idempotencyKey,
                              Map<String, Object> metadata) {
        String selectSql = "select overage_id from tenant_overage where tenant_id=:t and idempotency_key=:k";
        UUID existing = template.query(selectSql, Map.of("t", tenantId, "k", idempotencyKey), rs -> {
            if (rs.next()) {
                return UUID.fromString(rs.getString("overage_id"));
            }
            return null;
        });
        if (existing != null) {
            return existing;
        }

        UUID overageId = UUID.randomUUID();
        PGobject json = new PGobject();
        try {
            json.setType("jsonb");
            json.setValue(mapper.writeValueAsString(metadata));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize metadata", e);
        }

        String insertSql = "insert into tenant_overage (overage_id, tenant_id, subscription_id, feature_key, quantity, " +
                "unit_price_minor, currency, occurred_at, period_start, period_end, status, idempotency_key, metadata) " +
                "values (:oid, :t, :s, :f, :q, :p, :c, now(), :ps, :pe, 'RECORDED', :k, :m)";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("oid", overageId)
                .addValue("t", tenantId)
                .addValue("s", subscriptionId)
                .addValue("f", featureKey)
                .addValue("q", quantity)
                .addValue("p", unitPriceMinor)
                .addValue("c", currency)
                .addValue("ps", periodStart)
                .addValue("pe", periodEnd)
                .addValue("k", idempotencyKey)
                .addValue("m", json);
        template.update(insertSql, params);
        return overageId;
    }
}
