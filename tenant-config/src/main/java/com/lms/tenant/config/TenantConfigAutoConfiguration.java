package com.lms.tenant.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@AutoConfiguration
@EnableConfigurationProperties(TenantResolutionProperties.class)
@ConditionalOnClass({JdbcTemplate.class, TenantResolverFilter.class})
public class TenantConfigAutoConfiguration {

  @Bean
  public TenantResolverService tenantResolverService(org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate jdbc) {
    return new TenantResolverService(jdbc);
  }

  @Bean
  public TenantResolverFilter tenantResolverFilter(JdbcTemplate jdbc, TenantResolverService resolver, TenantResolutionProperties props) {
    return new TenantResolverFilter(jdbc, resolver, props);
  }
}
