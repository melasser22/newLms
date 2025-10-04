package com.ejada.gateway.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import com.ejada.gateway.config.GatewayTracingProperties;
import com.ejada.gateway.context.GatewayRequestAttributes;
import com.ejada.gateway.filter.GatewayMetricsFilter;
import com.ejada.gateway.observability.GatewayTracingHelper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class GatewayMetricsFilterTest {

  private SimpleMeterRegistry meterRegistry;
  private GatewayMetricsFilter filter;

  @BeforeEach
  void setUp() {
    this.meterRegistry = new SimpleMeterRegistry();
    GatewayTracingProperties tracingProperties = new GatewayTracingProperties();
    GatewayTracingHelper tracingHelper = new GatewayTracingHelper(null, tracingProperties);
    this.filter = new GatewayMetricsFilter(meterRegistry, tracingHelper);
  }

  @Test
  void recordsRequestMetricsPerTenantAndRoute() {
    MockServerHttpRequest request = MockServerHttpRequest.get("/api/students").build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);
    exchange.getAttributes().put(GatewayRequestAttributes.TENANT_ID, "acme");
    Route route = Route.async()
        .id("students-route")
        .uri(URI.create("http://students"))
        .predicate(serverWebExchange -> true)
        .build();
    exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, route);

    WebFilterChain chain = serverWebExchange -> {
      serverWebExchange.getResponse().setStatusCode(HttpStatus.NO_CONTENT);
      return Mono.empty();
    };

    StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

    assertThat(meterRegistry.find("gateway.requests.by_tenant").counter()).isNotNull();
    assertThat(meterRegistry.find("gateway.requests.by_tenant").counter().count()).isEqualTo(1d);

    assertThat(meterRegistry.find("gateway.route.latency").timer()).isNotNull();
    assertThat(meterRegistry.find("gateway.route.latency").timer().count()).isEqualTo(1L);

    assertThat(meterRegistry.find("gateway.request.duration").timer()).isNotNull();
  }
}
