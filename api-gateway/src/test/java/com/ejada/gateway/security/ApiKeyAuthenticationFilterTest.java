package com.ejada.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.ejada.common.constants.HeaderNames;
import com.ejada.gateway.config.GatewayRoutesProperties;
import com.ejada.gateway.config.GatewaySecurityProperties;
import com.ejada.gateway.config.GatewaySecurityProperties.EncryptionAlgorithm;
import com.ejada.gateway.context.GatewayRequestAttributes;
import com.ejada.gateway.security.apikey.ApiKeyCodec;
import com.ejada.gateway.support.ReactiveRedisTestSupport;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class ApiKeyAuthenticationFilterTest {

  private ReactiveStringRedisTemplate redisTemplate;
  private ReactiveRedisTestSupport.InMemoryRedisStore redisStore;
  private ApiKeyAuthenticationFilter filter;
  private SimpleMeterRegistry meterRegistry;
  private ObjectMapper objectMapper;
  private GatewaySecurityProperties properties;
  private GatewayRoutesProperties routesProperties;
  private ApiKeyCodec apiKeyCodec;
  private byte[] encryptionKey;
  private final SecureRandom secureRandom = new SecureRandom();

  @BeforeEach
  void setUp() {
    this.redisStore = ReactiveRedisTestSupport.newStore();
    this.redisTemplate = ReactiveRedisTestSupport.mockStringTemplate(redisStore);
    this.objectMapper = new ObjectMapper().findAndRegisterModules();
    this.meterRegistry = new SimpleMeterRegistry();
    GatewaySecurityMetrics metrics = new GatewaySecurityMetrics(meterRegistry);
    this.properties = new GatewaySecurityProperties();
    this.routesProperties = new GatewayRoutesProperties();
    configureRoutes();
    configureSecurity();
    this.apiKeyCodec = new ApiKeyCodec(properties, TestObjectProviders.of(objectMapper),
        TestObjectProviders.of(objectMapper), TestObjectProviders.of(secureRandom));
    this.filter = new ApiKeyAuthenticationFilter(redisTemplate, metrics, properties, routesProperties,
        TestObjectProviders.of(objectMapper), TestObjectProviders.of(objectMapper), apiKeyCodec);
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
  void authenticatesValidApiKeyAndPopulatesContext() throws Exception {
    Instant now = Instant.now();
    storeEncryptedRecord("test-key", Map.of(
        "tenantId", "tenant-a",
        "scopes", List.of("read"),
        "expiresAt", now.plusSeconds(120).toString(),
        "rotatedAt", now.toString()));

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
    assertThat((String) exchange.getAttribute(GatewayRequestAttributes.TENANT_ID)).isEqualTo("tenant-a");
    assertThat(authenticationRef.get()).isInstanceOf(ApiKeyAuthenticationToken.class);
    double validated = meterRegistry.get("gateway.security.api_key_validated").counter().count();
    assertThat(validated).isEqualTo(1.0d);

    Map<String, Object> stored = decryptRecord(redisTemplate.opsForValue().get("gateway:api-key:test-key").block());
    assertThat(stored.get("lastUsedAt")).isNotNull();
  }

  @Test
  void rejectsApiKeyWhenRequiredScopeMissing() throws Exception {
    Instant now = Instant.now();
    storeEncryptedRecord("missing-scope", Map.of(
        "tenantId", "tenant-b",
        "scopes", List.of("write"),
        "expiresAt", now.plusSeconds(60).toString(),
        "rotatedAt", now.toString()));

    MockServerHttpRequest request = MockServerHttpRequest.get("/secure")
        .header(HeaderNames.API_KEY, "missing-scope")
        .build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    StepVerifier.create(filter.filter(exchange, webExchange -> Mono.empty())).verifyComplete();

    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    String body = exchange.getResponse().getBodyAsString().block();
    assertThat(body).contains("ERR_API_KEY_SCOPE");
  }

  @Test
  void rejectsApiKeyWhenRotationExpired() throws Exception {
    Instant now = Instant.now();
    storeEncryptedRecord("expired-rotation", Map.of(
        "tenantId", "tenant-c",
        "scopes", List.of("read"),
        "expiresAt", now.plusSeconds(60).toString(),
        "rotatedAt", now.minusSeconds(91L * 24 * 3600).toString()));

    MockServerHttpRequest request = MockServerHttpRequest.get("/secure")
        .header(HeaderNames.API_KEY, "expired-rotation")
        .build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    StepVerifier.create(filter.filter(exchange, webExchange -> Mono.empty())).verifyComplete();

    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    String body = exchange.getResponse().getBodyAsString().block();
    assertThat(body).contains("ERR_API_KEY_ROTATED");
  }

  @Test
  void rejectsUnknownApiKey() {
    MockServerHttpRequest request = MockServerHttpRequest.get("/secure")
        .header(HeaderNames.API_KEY, "missing-key")
        .build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    StepVerifier.create(filter.filter(exchange, webExchange -> Mono.empty())).verifyComplete();

    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    String body = exchange.getResponse().getBodyAsString().block();
    assertThat(body).contains("ERR_API_KEY_INVALID");
    double blocked = meterRegistry.get("gateway.security.blocked").counter().count();
    assertThat(blocked).isEqualTo(1.0d);
  }

  private void configureRoutes() {
    GatewayRoutesProperties.ServiceRoute route = new GatewayRoutesProperties.ServiceRoute();
    route.setId("test-route");
    route.setPaths(List.of("/secure"));
    route.setMethods(List.of("GET"));
    route.setRequiredScopes(List.of("read"));
    routesProperties.getRoutes().put("test-route", route);
  }

  private void configureSecurity() {
    GatewaySecurityProperties.ApiKey apiKey = properties.getApiKey();
    apiKey.setEnabled(true);
    apiKey.getRotation().setEnabled(true);
    apiKey.getRotation().setMaxAgeDays(90);
    apiKey.getScopeEnforcement().setEnabled(true);
    apiKey.getAudit().setLogUsage(true);
    apiKey.getAudit().setTrackLastUsed(true);
    apiKey.getEncryption().setEnabled(true);
    apiKey.getEncryption().setAlgorithm(EncryptionAlgorithm.AES_256_GCM);
    this.encryptionKey = new byte[32];
    secureRandom.nextBytes(this.encryptionKey);
    apiKey.getEncryption().setKeyValue(Base64.getEncoder().encodeToString(encryptionKey));
    apiKey.getEncryption().setKeyId("unit-test");
  }

  private void storeEncryptedRecord(String apiKey, Map<String, Object> record) throws Exception {
    byte[] plaintext = objectMapper.writeValueAsBytes(record);
    byte[] iv = new byte[12];
    secureRandom.nextBytes(iv);
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(encryptionKey, "AES"), new GCMParameterSpec(128, iv));
    byte[] ciphertext = cipher.doFinal(plaintext);
    String payload = objectMapper.writeValueAsString(Map.of(
        "ciphertext", Base64.getEncoder().encodeToString(ciphertext),
        "iv", Base64.getEncoder().encodeToString(iv),
        "algorithm", properties.getApiKey().getEncryption().getAlgorithm().getId(),
        "keyId", properties.getApiKey().getEncryption().getKeyId()));
    redisTemplate.opsForValue().set(properties.getApiKey().redisKey(apiKey), payload).block();
  }

  private Map<String, Object> decryptRecord(String payload) throws Exception {
    JsonNode node = objectMapper.readTree(payload);
    if (node.hasNonNull("ciphertext")) {
      byte[] ciphertext = Base64.getDecoder().decode(node.get("ciphertext").asText());
      byte[] iv = Base64.getDecoder().decode(node.get("iv").asText());
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(encryptionKey, "AES"), new GCMParameterSpec(128, iv));
      byte[] plaintext = cipher.doFinal(ciphertext);
      return objectMapper.readValue(plaintext, new TypeReference<Map<String, Object>>() {});
    }
    return objectMapper.readValue(payload, new TypeReference<Map<String, Object>>() {});
  }

}
