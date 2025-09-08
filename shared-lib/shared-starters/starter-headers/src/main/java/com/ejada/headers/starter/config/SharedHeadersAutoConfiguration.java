package com.ejada.headers.starter.config;

import com.ejada.headers.starter.http.CorrelationHeaderFilter;
import com.ejada.headers.starter.http.SecurityHeadersFilter;
import jakarta.servlet.Filter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.filter.ForwardedHeaderFilter;

@AutoConfiguration
@EnableConfigurationProperties(SharedHeadersProperties.class)
@PropertySource("classpath:/com/ejada/headers/starter/headers-defaults.properties")
@ConditionalOnProperty(prefix = "shared.headers", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SharedHeadersAutoConfiguration {

  // Inbound servlet filters
  @Bean
  @ConditionalOnClass(Filter.class)
  @ConditionalOnMissingBean(name = "correlationHeaderFilter")
  public CorrelationHeaderFilter correlationHeaderFilter(SharedHeadersProperties props,
                                                        @Value("${correlation.header.compatibility.enabled:true}") boolean compatibility) {
    return new CorrelationHeaderFilter(props, compatibility);
  }

  @Bean
  @ConditionalOnClass(Filter.class)
  @ConditionalOnMissingBean(name = "securityHeadersFilter")
  @ConditionalOnProperty(prefix = "shared.headers.security", name = "enabled", havingValue = "true", matchIfMissing = true)
  public SecurityHeadersFilter securityHeadersFilter(SharedHeadersProperties props) {
    return new SecurityHeadersFilter(props);
  }

  @Bean
  @ConditionalOnClass(ForwardedHeaderFilter.class)
  @ConditionalOnProperty(prefix = "shared.headers.forwarded", name = "enabled", havingValue = "true", matchIfMissing = true)
  public ForwardedHeaderFilter forwardedHeaderFilter() {
    return new ForwardedHeaderFilter();
  }

  // Outbound header propagation handled in WebClient auto-configuration
}
