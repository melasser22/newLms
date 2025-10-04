package com.ejada.gateway.config;

import com.ejada.common.constants.HeaderNames;
import com.ejada.gateway.context.GatewayRequestAttributes;
import io.netty.channel.ChannelOption;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.transport.logging.AdvancedByteBufFormat;

/**
 * Central {@link WebClient} configuration with sensible defaults for
 * downstream service calls. The builder is load-balanced so URIs with the
 * {@code lb://} scheme resolve via Spring Cloud LoadBalancer/Eureka.
 */
@Configuration
@EnableConfigurationProperties(GatewayWebClientProperties.class)
public class WebClientConfig {

  @Bean
  public ClientHttpConnector gatewayClientHttpConnector(GatewayWebClientProperties properties,
      GatewayOptimizationProperties optimizationProperties) {
    HttpClient httpClient = createHttpClient(properties, optimizationProperties);
    Duration connectTimeout = properties.getConnectTimeout();
    if (connectTimeout != null) {
      httpClient = httpClient.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) connectTimeout.toMillis());
    }
    Duration responseTimeout = properties.getResponseTimeout();
    if (responseTimeout != null) {
      httpClient = httpClient.responseTimeout(responseTimeout);
    }
    Duration readTimeout = properties.getReadTimeout();
    Duration writeTimeout = properties.getWriteTimeout();
    httpClient = httpClient.doOnConnected(connection -> {
      if (readTimeout != null && !readTimeout.isZero() && !readTimeout.isNegative()) {
        connection.addHandlerLast(new ReadTimeoutHandler(readTimeout.toMillis(), TimeUnit.MILLISECONDS));
      }
      if (writeTimeout != null && !writeTimeout.isZero() && !writeTimeout.isNegative()) {
        connection.addHandlerLast(new WriteTimeoutHandler(writeTimeout.toMillis(), TimeUnit.MILLISECONDS));
      }
    });

    if (properties.isCompress()) {
      httpClient = httpClient.compress(true);
    }
    if (properties.isWiretap()) {
      httpClient = httpClient.wiretap("gateway-webclient", LogLevel.DEBUG, AdvancedByteBufFormat.TEXTUAL);
    }
    return new ReactorClientHttpConnector(httpClient);
  }

  private HttpClient createHttpClient(GatewayWebClientProperties properties,
      GatewayOptimizationProperties optimizationProperties) {
    HttpClient httpClient;
    if (optimizationProperties.isEnabled()) {
      GatewayOptimizationProperties.Pool pool = properties.getPool();
      ConnectionProvider.Builder builder = ConnectionProvider.builder("gateway-connection-pool")
          .maxConnections(pool.getMaxConnections())
          .pendingAcquireMaxCount(pool.getPendingAcquireMaxCount())
          .pendingAcquireTimeout(pool.getAcquireTimeout());
      if (pool.getMaxIdleTime() != null) {
        builder.maxIdleTime(pool.getMaxIdleTime());
      }
      if (pool.getMaxLifeTime() != null) {
        builder.maxLifeTime(pool.getMaxLifeTime());
      }
      if (pool.getEvictInBackground() != null) {
        builder.evictInBackground(pool.getEvictInBackground());
      }
      if (pool.isMetricsEnabled()) {
        builder.metrics(true);
      }
      httpClient = HttpClient.create(builder.build()).keepAlive(true);
    } else {
      httpClient = HttpClient.create();
    }
    return httpClient;
  }

  @Bean
  @LoadBalanced
  public WebClient.Builder loadBalancedWebClientBuilder(ClientHttpConnector gatewayClientHttpConnector,
      GatewayWebClientProperties properties,
      ObjectProvider<ExchangeFilterFunction> customFilters) {
    WebClient.Builder builder = WebClient.builder()
        .clientConnector(gatewayClientHttpConnector)
        .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(properties.getMaxInMemorySize()))
        .filter(contextPropagationFilter());

    customFilters.orderedStream().forEach(builder::filter);
    return builder;
  }

  private ExchangeFilterFunction contextPropagationFilter() {
    return (request, next) -> Mono.deferContextual(contextView -> {
      ClientRequest.Builder builder = ClientRequest.from(request);
      propagate(contextView, builder, GatewayRequestAttributes.CORRELATION_ID, HeaderNames.CORRELATION_ID);
      propagate(contextView, builder, GatewayRequestAttributes.TENANT_ID, HeaderNames.X_TENANT_ID);
      propagate(contextView, builder, HeaderNames.USER_ID, HeaderNames.USER_ID);
      propagate(contextView, builder, GatewayRequestAttributes.API_VERSION, HeaderNames.API_VERSION);
      return next.exchange(builder.build());
    });
  }

  private void propagate(reactor.util.context.ContextView contextView, ClientRequest.Builder builder,
      String contextKey, String headerName) {
    if (!contextView.hasKey(contextKey)) {
      return;
    }
    Object value = contextView.get(contextKey);
    if (value == null) {
      return;
    }
    String text = Objects.toString(value, null);
    if (text == null || text.isBlank()) {
      return;
    }
    builder.headers(httpHeaders -> {
      if (!httpHeaders.containsKey(headerName)) {
        httpHeaders.add(headerName, text);
      }
    });
  }
}

