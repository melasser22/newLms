package com.ejada.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.ejada.common.constants.HeaderNames;
import com.ejada.gateway.config.GatewaySecurityProperties;
import com.ejada.gateway.context.GatewayRequestAttributes;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class ApiKeyAuthenticationFilterTest {

  @Container
  static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7").withExposedPorts(6379);

  private ReactiveStringRedisTemplate redisTemplate;
  private ReactiveRedisConnectionFactory connectionFactory;
  private ApiKeyAuthenticationFilter filter;
  private SimpleMeterRegistry meterRegistry;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    LettuceConnectionFactory factory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
    factory.afterPropertiesSet();
    this.connectionFactory = factory;
    this.redisTemplate = new ReactiveStringRedisTemplate(factory);
    flushRedis();

    this.objectMapper = new ObjectMapper().findAndRegisterModules();
    this.meterRegistry = new SimpleMeterRegistry();
    GatewaySecurityMetrics metrics = new GatewaySecurityMetrics(meterRegistry);
    GatewaySecurityProperties properties = new GatewaySecurityProperties();
    properties.getApiKey().setEnabled(true);
    this.filter = new ApiKeyAuthenticationFilter(redisTemplate, metrics, properties, TestObjectProviders.of(objectMapper), TestObjectProviders.of(objectMapper));
  }

  @AfterEach
  void tearDown() {
    if (connectionFactory instanceof LettuceConnectionFactory lettuce) {
      lettuce.destroy();
    }
    if (meterRegistry != null) {
      meterRegistry.close();
    }
  }

  @Test
  void authenticatesValidApiKeyAndPopulatesContext() throws Exception {
    Instant expiresAt = Instant.now().plusSeconds(120);
    String value = objectMapper.writeValueAsString(Map.of(
        "tenantId", "tenant-a",
        "scopes", List.of("read"),
        "expiresAt", expiresAt.toString()));
    redisTemplate.opsForValue().set("gateway:api-key:test-key", value).block();

    MockServerHttpRequest request = MockServerHttpRequest.get("/secure")
        .header(HeaderNames.API_KEY, "test-key")
        .build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    AtomicReference<Authentication> authenticationRef = new AtomicReference<>();
    WebFilterChain chain = webExchange -> Mono.deferContextual(ctx -> {
      SecurityContext securityContext = ctx.getOrDefault(SecurityContext.class, null);
      if (securityContext != null) {
        authenticationRef.set(securityContext.getAuthentication());
      }
      return Mono.empty();
    });

    StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

    assertThat(exchange.getRequest().getHeaders().getFirst(HeaderNames.X_TENANT_ID)).isEqualTo("tenant-a");
    assertThat(exchange.getAttribute(GatewayRequestAttributes.TENANT_ID)).isEqualTo("tenant-a");
    assertThat(authenticationRef.get()).isInstanceOf(ApiKeyAuthenticationToken.class);
    double validated = meterRegistry.get("gateway.security.api_key_validated").counter().count();
    assertThat(validated).isEqualTo(1.0d);
  }

  @Test
  void rejectsUnknownApiKey() {
    MockServerHttpRequest request = MockServerHttpRequest.get("/secure")
        .header(HeaderNames.API_KEY, "missing-key")
        .build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    WebFilterChain chain = webExchange -> Mono.empty();

    StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    String body = exchange.getResponse().getBodyAsString().block();
    assertThat(body).contains("ERR_API_KEY_INVALID");
    double blocked = meterRegistry.get("gateway.security.blocked").counter().count();
    assertThat(blocked).isEqualTo(1.0d);
  }

  private void flushRedis() {
    try (var connection = connectionFactory.getReactiveConnection()) {
      connection.serverCommands().flushAll().block();
    }
  }
}
