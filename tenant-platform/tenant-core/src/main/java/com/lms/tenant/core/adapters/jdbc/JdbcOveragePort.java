package com.lms.tenant.core.adapters.jdbc;

import com.lms.tenant.core.OveragePort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@ConditionalOnMissingBean(OveragePort.class)
public class JdbcOveragePort implements OveragePort {
    private final NamedParameterJdbcTemplate jdbc;

    public JdbcOveragePort(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public UUID recordOverage(UUID tenantId, UUID subscriptionId, String featureKey, long quantity, long unitPriceMinor, String currency, Instant periodStart, Instant periodEnd, String idempotencyKey) {
        throw new UnsupportedOperationException("JDBC overage insert not implemented");
    }
}
