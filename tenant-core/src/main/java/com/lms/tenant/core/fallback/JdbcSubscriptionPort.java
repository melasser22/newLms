package com.lms.tenant.core.fallback;

import com.lms.tenant.core.port.SubscriptionPort;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

class JdbcSubscriptionPort implements SubscriptionPort {

    private final NamedParameterJdbcTemplate template;

    JdbcSubscriptionPort(NamedParameterJdbcTemplate template) {
        this.template = template;
    }

    @Override
    public ActiveSubscription loadActive(UUID tenantId) {
        String sql = "select subscription_id, tier_id, period_start, period_end " +
                "from tenant_subscription " +
                "where tenant_id=:t and status in ('TRIALING','ACTIVE','PAST_DUE') " +
                "order by created_at desc limit 1";
        return template.queryForObject(sql, Map.of("t", tenantId), (rs, rowNum) -> map(rs));
    }

    private ActiveSubscription map(ResultSet rs) throws SQLException {
        UUID id = UUID.fromString(rs.getString("subscription_id"));
        String tier = rs.getString("tier_id");
        Instant start = rs.getTimestamp("period_start").toInstant();
        Instant end = rs.getTimestamp("period_end").toInstant();
        return new ActiveSubscription(id, tier, start, end);
    }
}
