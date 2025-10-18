package com.ejada.shared_starter_resilience;

import io.netty.channel.ChannelOption;
import java.time.Duration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/**
 * Auto-configuration for basic HTTP resilience features.
 *
 * <p>This configuration provides a {@link RestTemplateBuilder} that sets
 * connect and read timeouts based on {@link SharedResilienceProps}. It relies
 * solely on the blocking {@link RestTemplate} API.</p>
 */
@AutoConfiguration
@EnableConfigurationProperties(SharedResilienceProps.class)
public class ResilienceAutoConfiguration {

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass(RestTemplateBuilder.class)
  static class BlockingRestTemplateConfiguration {

    @Bean
    @ConditionalOnMissingBean(RestTemplateBuilder.class)
    RestTemplateBuilder resilientRestTemplateBuilder(SharedResilienceProps props) {
      return new RestTemplateBuilder()
          .requestFactory(
              () -> {
                SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
                factory.setConnectTimeout((int) props.getConnectTimeoutMs());
                factory.setReadTimeout((int) props.getHttpTimeoutMs());
                return factory;
              });
    }

    @Bean
    @ConditionalOnMissingBean(RestTemplate.class)
    RestTemplate resilientRestTemplate(RestTemplateBuilder builder) {
      return builder.build();
    }
  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass(name = "org.springframework.web.reactive.function.client.WebClient")
  static class ReactiveWebClientConfiguration {

    @Bean
    @ConditionalOnMissingBean(WebClient.Builder.class)
    WebClient.Builder resilientWebClientBuilder(SharedResilienceProps props) {
      HttpClient httpClient = HttpClient.create()
          .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, props.getConnectTimeoutMs())
          .responseTimeout(Duration.ofMillis(props.getHttpTimeoutMs()));
      return WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient));
    }

    @Bean
    @ConditionalOnMissingBean(WebClient.class)
    WebClient resilientWebClient(WebClient.Builder builder) {
      return builder.build();
    }
  }
}

