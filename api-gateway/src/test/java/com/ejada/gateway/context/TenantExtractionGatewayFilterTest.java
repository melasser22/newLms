package com.ejada.gateway.context;

import static org.assertj.core.api.Assertions.assertThat;

import com.ejada.common.constants.HeaderNames;
import com.ejada.common.context.ReactiveContextHolder;
import com.ejada.gateway.context.GatewayRequestAttributes;
import com.ejada.starter_core.config.CoreAutoConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.context.ContextView;

class TenantExtractionGatewayFilterTest {

  private TenantExtractionGatewayFilter filter;

  @BeforeEach
  void setUp() {
    CoreAutoConfiguration.CoreProps props = new CoreAutoConfiguration.CoreProps();
    props.getTenant().setSkipPatterns(new String[0]);
    filter = new TenantExtractionGatewayFilter(props, new ObjectMapper());
  }

  @Test
  void populatesReactorContextWithResolvedTenant() {
    MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/data?tenantId=acme")
        .build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);
    exchange.getAttributes().put(GatewayRequestAttributes.CORRELATION_ID, "corr-123");

    WebFilterChain chain = serverExchange -> Mono.deferContextual((ContextView ctx) -> {
      assertThat((String) ctx.get(GatewayRequestAttributes.TENANT_ID)).isEqualTo("acme");
      assertThat((String) ctx.get(HeaderNames.X_TENANT_ID)).isEqualTo("acme");
      assertThat((String) ctx.get(ReactiveContextHolder.TENANT_CONTEXT_KEY)).isEqualTo("acme");
      assertThat((String) ctx.get(GatewayRequestAttributes.CORRELATION_ID)).isEqualTo("corr-123");
      assertThat((String) ctx.get(HeaderNames.CORRELATION_ID)).isEqualTo("corr-123");
      return Mono.empty();
    });

    StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
  }
}
