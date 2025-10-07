package com.ejada.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Provides a {@link WebClient} bean backed by the load-balanced builder so components that
 * inject {@link WebClient} directly continue to benefit from the {@code lb://} resolution.
 */
@Configuration(proxyBeanMethods = false)
public class GatewayWebClientConfiguration {

  @Bean
  @Primary
  public WebClient gatewayWebClient(WebClient.Builder loadBalancedWebClientBuilder) {
    return loadBalancedWebClientBuilder.clone().build();
  }
}
