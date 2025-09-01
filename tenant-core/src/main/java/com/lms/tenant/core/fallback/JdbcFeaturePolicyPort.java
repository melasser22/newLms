package com.lms.tenant.core.fallback;

import com.lms.tenant.core.port.FeaturePolicyPort;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

class JdbcFeaturePolicyPort implements FeaturePolicyPort {

    private final NamedParameterJdbcTemplate template;

    JdbcFeaturePolicyPort(NamedParameterJdbcTemplate template) {
        this.template = template;
    }

    @Override
    public EffectiveFeature effective(String tierId, UUID tenantId, String featureKey) {
        String sql = "select coalesce(o.enabled, t.enabled) as enabled, " +
                "       coalesce(o.limit_value, t.limit_value) as limit_value, " +
                "       coalesce(o.allow_overage_override, t.allow_overage) as allow_overage, " +
                "       coalesce(o.overage_unit_price_minor_override, t.overage_unit_price_minor) as price, " +
                "       coalesce(o.overage_currency_override, t.overage_currency) as currency " +
                "from tier_feature_limit t " +
                "left join tenant_feature_override o on o.tenant_id=:tid and o.feature_key=:f " +
                "where t.tier_id=:tier and t.feature_key=:f";
        return template.queryForObject(sql, Map.of("tid", tenantId, "tier", tierId, "f", featureKey), (rs, rowNum) ->
                new EffectiveFeature(rs.getBoolean("enabled"),
                        rs.getObject("limit_value") == null ? null : rs.getLong("limit_value"),
                        rs.getBoolean("allow_overage"),
                        rs.getObject("price") == null ? null : rs.getLong("price"),
                        Optional.ofNullable(rs.getString("currency")).orElse("USD")));
    }
}
