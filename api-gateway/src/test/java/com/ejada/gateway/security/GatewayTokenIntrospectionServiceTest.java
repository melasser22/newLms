package com.ejada.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.ejada.common.dto.BaseResponse;
import com.ejada.gateway.config.GatewaySecurityProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class GatewayTokenIntrospectionServiceTest {

  @Container
  static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7").withExposedPorts(6379);

  private ReactiveStringRedisTemplate redisTemplate;
  private ReactiveRedisConnectionFactory connectionFactory;
  private SimpleMeterRegistry meterRegistry;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    LettuceConnectionFactory factory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
    factory.afterPropertiesSet();
    this.connectionFactory = factory;
    this.redisTemplate = new ReactiveStringRedisTemplate(factory);
    flushRedis();
    this.meterRegistry = new SimpleMeterRegistry();
    this.objectMapper = new ObjectMapper().findAndRegisterModules();
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
  void cachesSuccessfulIntrospection() {
    GatewaySecurityProperties properties = new GatewaySecurityProperties();
    properties.getTokenCache().setEnabled(true);
    properties.getTokenCache().setTtl(Duration.ofMinutes(5));

    AtomicInteger calls = new AtomicInteger();
    WebClient.Builder builder = WebClient.builder().exchangeFunction(request -> {
      calls.incrementAndGet();
      Map<String, Object> data = Map.of(
          "active", true,
          "expiresAt", Instant.now().plusSeconds(120).toString(),
          "tenantId", "tenant-a");
      BaseResponse<Map<String, Object>> response = BaseResponse.success(data);
      String json;
      try {
        json = objectMapper.writeValueAsString(response);
      } catch (Exception ex) {
        throw new IllegalStateException(ex);
      }
      ClientResponse clientResponse = ClientResponse.create(HttpStatus.OK)
          .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .body(json)
          .build();
      return Mono.just(clientResponse);
    });

    GatewaySecurityMetrics metrics = new GatewaySecurityMetrics(meterRegistry);
    GatewayTokenIntrospectionService service = new GatewayTokenIntrospectionService(properties, redisTemplate, metrics,
        TestObjectProviders.of(objectMapper), TestObjectProviders.of(objectMapper), builder);

    Jwt jwt = jwt();

    StepVerifier.create(service.verify("token", jwt)).verifyComplete();
    StepVerifier.create(service.verify("token", jwt)).verifyComplete();

    assertThat(calls.get()).isEqualTo(1);
    String cacheKey = properties.getTokenCache().redisKey("jti-123");
    assertThat(redisTemplate.opsForValue().get(cacheKey).block()).isNotNull();
  }

  @Test
  void errorsWhenTokenRevoked() {
    GatewaySecurityProperties properties = new GatewaySecurityProperties();
    properties.getTokenCache().setEnabled(true);
    properties.getTokenCache().setTtl(Duration.ofMinutes(5));

    WebClient.Builder builder = WebClient.builder().exchangeFunction(request -> {
      Map<String, Object> data = Map.of(
          "active", false,
          "tenantId", "tenant-a");
      BaseResponse<Map<String, Object>> response = BaseResponse.success(data);
      String json;
      try {
        json = objectMapper.writeValueAsString(response);
      } catch (Exception ex) {
        throw new IllegalStateException(ex);
      }
      ClientResponse clientResponse = ClientResponse.create(HttpStatus.OK)
          .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .body(json)
          .build();
      return Mono.just(clientResponse);
    });

    GatewaySecurityMetrics metrics = new GatewaySecurityMetrics(meterRegistry);
    GatewayTokenIntrospectionService service = new GatewayTokenIntrospectionService(properties, redisTemplate, metrics,
        TestObjectProviders.of(objectMapper), TestObjectProviders.of(objectMapper), builder);

    StepVerifier.create(service.verify("token", jwt()))
        .expectError(JwtException.class)
        .verify();

    assertThat(meterRegistry.get("gateway.security.blocked").counter().count()).isEqualTo(1.0);
  }

  private Jwt jwt() {
    Instant now = Instant.now();
    return Jwt.withTokenValue("token")
        .header("alg", "HS256")
        .claim("jti", "jti-123")
        .issuedAt(now)
        .expiresAt(now.plusSeconds(120))
        .build();
  }

  private void flushRedis() {
    try (var connection = connectionFactory.getReactiveConnection()) {
      connection.serverCommands().flushAll().block();
    }
  }
}
