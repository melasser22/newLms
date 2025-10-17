package com.ejada.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.ejada.common.constants.HeaderNames;
import com.ejada.gateway.config.GatewaySecurityProperties;
import com.ejada.gateway.support.ReactiveRedisTestSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.net.InetSocketAddress;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class IpFilteringGatewayFilterTest {

  private ReactiveStringRedisTemplate redisTemplate;
  private ReactiveRedisTestSupport.InMemoryRedisStore redisStore;
  private IpFilteringGatewayFilter filter;
  private SimpleMeterRegistry meterRegistry;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    this.redisStore = ReactiveRedisTestSupport.newStore();
    this.redisTemplate = ReactiveRedisTestSupport.mockStringTemplate(redisStore);
    this.objectMapper = new ObjectMapper().findAndRegisterModules();
    this.meterRegistry = new SimpleMeterRegistry();
    GatewaySecurityMetrics metrics = new GatewaySecurityMetrics(meterRegistry);
    GatewaySecurityProperties properties = new GatewaySecurityProperties();
    properties.getIpFiltering().setEnabled(true);
    this.filter = new IpFilteringGatewayFilter(redisTemplate, properties, metrics, TestObjectProviders.of(objectMapper), TestObjectProviders.of(objectMapper));
  }

  @AfterEach
  void tearDown() {
    if (redisStore != null) {
      redisStore.clear();
    }
    if (meterRegistry != null) {
      meterRegistry.close();
    }
  }

  @Test
  void blocksBlacklistedIp() {
    redisTemplate.opsForSet().add("gateway:tenant:tenant-a:ip-blacklist", "1.2.3.4").block();

    MockServerHttpRequest request = MockServerHttpRequest.get("/secure")
        .remoteAddress(new InetSocketAddress("1.2.3.4", 0))
        .header(HeaderNames.X_TENANT_ID, "tenant-a")
        .build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    GatewayFilterChain chain = webExchange -> Mono.empty();

    StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    String body = exchange.getResponse().getBodyAsString().block();
    assertThat(body).contains("ERR_IP_BLOCKED");
    assertThat(meterRegistry.get("gateway.security.blocked").counter().count()).isEqualTo(1.0);
  }

  @Test
  void enforcesWhitelistWhenConfigured() {
    redisTemplate.opsForSet().add("gateway:tenant:tenant-a:ip-whitelist", "5.6.7.8").block();

    MockServerHttpRequest request = MockServerHttpRequest.get("/secure")
        .remoteAddress(new InetSocketAddress("9.9.9.9", 0))
        .header(HeaderNames.X_TENANT_ID, "tenant-a")
        .build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    GatewayFilterChain chain = webExchange -> Mono.empty();

    StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(exchange.getResponse().getBodyAsString().block()).contains("ERR_IP_BLOCKED");
  }

  @Test
  void allowsRequestWhenIpInWhitelist() {
    redisTemplate.opsForSet().add("gateway:tenant:tenant-a:ip-whitelist", "5.6.7.8").block();

    MockServerHttpRequest request = MockServerHttpRequest.get("/secure")
        .remoteAddress(new InetSocketAddress("5.6.7.8", 0))
        .header(HeaderNames.X_TENANT_ID, "tenant-a")
        .build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    GatewayFilterChain chain = webExchange -> Mono.empty();

    StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

    assertThat(exchange.getResponse().getStatusCode()).isNull();
  }

  @Test
  void allowsCidrMatches() {
    redisTemplate.opsForSet().add("gateway:tenant:tenant-a:ip-whitelist", "10.0.0.0/24").block();

    MockServerHttpRequest request = MockServerHttpRequest.get("/secure")
        .remoteAddress(new InetSocketAddress("10.0.0.42", 0))
        .header(HeaderNames.X_TENANT_ID, "tenant-a")
        .build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    GatewayFilterChain chain = webExchange -> Mono.empty();

    StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

    assertThat(exchange.getResponse().getStatusCode()).isNull();
  }

}
