package com.lms.tenant.core.fallback;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.tenant.core.port.FeaturePolicyPort;
import com.lms.tenant.core.port.OveragePort;
import com.lms.tenant.core.port.SubscriptionPort;
import com.lms.tenant.core.port.TenantSettingsPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@Configuration
public class JdbcFallbackConfig {

    @Bean
    @ConditionalOnMissingBean(SubscriptionPort.class)
    SubscriptionPort jdbcSubscriptionPort(NamedParameterJdbcTemplate template) {
        return new JdbcSubscriptionPort(template);
    }

    @Bean
    @ConditionalOnMissingBean(FeaturePolicyPort.class)
    FeaturePolicyPort jdbcFeaturePolicyPort(NamedParameterJdbcTemplate template) {
        return new JdbcFeaturePolicyPort(template);
    }

    @Bean
    @ConditionalOnMissingBean(TenantSettingsPort.class)
    TenantSettingsPort jdbcTenantSettingsPort(NamedParameterJdbcTemplate template) {
        return new JdbcTenantSettingsPort(template);
    }

    @Bean
    @ConditionalOnMissingBean(OveragePort.class)
    OveragePort jdbcOveragePort(NamedParameterJdbcTemplate template, ObjectMapper mapper) {
        return new JdbcOveragePort(template, mapper);
    }
}
