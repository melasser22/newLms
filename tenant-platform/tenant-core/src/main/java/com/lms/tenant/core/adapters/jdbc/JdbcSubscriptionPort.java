package com.lms.tenant.core.adapters.jdbc;

import com.lms.tenant.core.SubscriptionPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConditionalOnMissingBean(SubscriptionPort.class)
public class JdbcSubscriptionPort implements SubscriptionPort {
    private final NamedParameterJdbcTemplate jdbc;

    public JdbcSubscriptionPort(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public ActiveSubscription activeSubscription(UUID tenantId) {
        throw new UnsupportedOperationException("JDBC subscription lookup not implemented");
    }
}
