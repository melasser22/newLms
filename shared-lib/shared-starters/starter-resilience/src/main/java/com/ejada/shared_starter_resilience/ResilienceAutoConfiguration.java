package com.ejada.shared_starter_resilience;

import java.time.Duration;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

/**
 * Auto-configuration for basic HTTP resilience features.
 *
 * <p>This configuration provides a {@link RestTemplateBuilder} that sets
 * connect and read timeouts based on {@link SharedResilienceProps}. It avoids
 * any dependency on WebFlux or reactive clients.</p>
 */
@AutoConfiguration
@ConditionalOnClass(RestTemplateBuilder.class)
@EnableConfigurationProperties(SharedResilienceProps.class)
public class ResilienceAutoConfiguration {

  /**
   * Creates a {@link RestTemplateBuilder} with timeouts derived from the
   * supplied {@link SharedResilienceProps}. The bean is only created if no
   * other {@code RestTemplateBuilder} is already present.
   *
   * @param props properties containing timeout values
   * @return a configured {@link RestTemplateBuilder}
   */
  @Bean
  @ConditionalOnMissingBean(RestTemplateBuilder.class)
  public RestTemplateBuilder resilientRestTemplateBuilder(SharedResilienceProps props) {
    return new RestTemplateBuilder()
        .setConnectTimeout(Duration.ofMillis(props.getConnectTimeoutMs()))
        .setReadTimeout(Duration.ofMillis(props.getHttpTimeoutMs()));
  }

  /**
   * Provides a {@link RestTemplate} built from the configured builder when no
   * other {@code RestTemplate} bean is defined.
   *
   * @param builder the builder configured with timeouts
   * @return a {@link RestTemplate} instance
   */
  @Bean
  @ConditionalOnMissingBean(RestTemplate.class)
  public RestTemplate resilientRestTemplate(RestTemplateBuilder builder) {
    return builder.build();
  }
}

