package com.ejada.tenant.core.adapters.jdbc;

import com.ejada.tenant.core.FeaturePolicyPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConditionalOnMissingBean(FeaturePolicyPort.class)
public class JdbcFeaturePolicyPort implements FeaturePolicyPort {
    private final NamedParameterJdbcTemplate jdbc;

    public JdbcFeaturePolicyPort(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public EffectiveFeature effective(String tierId, UUID tenantId, String featureKey) {
        throw new UnsupportedOperationException("JDBC feature policy lookup not implemented");
    }
}
