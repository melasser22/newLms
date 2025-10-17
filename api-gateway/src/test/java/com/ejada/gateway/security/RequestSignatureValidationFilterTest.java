package com.ejada.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.ejada.common.constants.HeaderNames;
import com.ejada.gateway.config.GatewaySecurityProperties;
import com.ejada.gateway.support.ReactiveRedisTestSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
class RequestSignatureValidationFilterTest {

  private ReactiveStringRedisTemplate redisTemplate;
  private ReactiveRedisTestSupport.InMemoryRedisStore redisStore;
  private RequestSignatureValidationFilter filter;
  private SimpleMeterRegistry meterRegistry;
  private ObjectMapper objectMapper;
  private final String secret = "top-secret";

  @BeforeEach
  void setUp() {
    this.redisStore = ReactiveRedisTestSupport.newStore();
    this.redisTemplate = ReactiveRedisTestSupport.mockStringTemplate(redisStore);
    this.objectMapper = new ObjectMapper().findAndRegisterModules();
    this.meterRegistry = new SimpleMeterRegistry();
    GatewaySecurityMetrics metrics = new GatewaySecurityMetrics(meterRegistry);
    GatewaySecurityProperties properties = new GatewaySecurityProperties();
    properties.getSignatureValidation().setEnabled(true);
    properties.getSignatureValidation().setSkipPatterns(new String[]{});
    redisTemplate.opsForValue().set(properties.getSignatureValidation().redisKey("tenant-a"), secret).block();
    this.filter = new RequestSignatureValidationFilter(redisTemplate, properties, metrics, TestObjectProviders.of(objectMapper), TestObjectProviders.of(objectMapper));
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
  void allowsRequestsWithValidSignature() throws Exception {
    String body = "{\"value\":42}";
    MockServerHttpRequest request = MockServerHttpRequest.post("/secure")
        .header(HeaderNames.X_TENANT_ID, "tenant-a")
        .header("X-Signature", hmac("POST\n/secure\n\n" + body))
        .body(body);
    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    java.util.concurrent.atomic.AtomicReference<String> captured = new java.util.concurrent.atomic.AtomicReference<>();
    WebFilterChain chain = webExchange -> DataBufferUtils.join(webExchange.getRequest().getBody())
        .flatMap(buffer -> {
          byte[] bytes = new byte[buffer.readableByteCount()];
          buffer.read(bytes);
          DataBufferUtils.release(buffer);
          captured.set(new String(bytes, StandardCharsets.UTF_8));
          return Mono.empty();
        });

    StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
    assertThat(captured.get()).isEqualTo(body);
  }

  @Test
  void rejectsWhenSignatureMissing() {
    MockServerHttpRequest request = MockServerHttpRequest.post("/secure")
        .header(HeaderNames.X_TENANT_ID, "tenant-a")
        .body("");
    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    WebFilterChain chain = webExchange -> Mono.empty();

    StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(exchange.getResponse().getBodyAsString().block()).contains("ERR_SIGNATURE_MISSING");
  }

  @Test
  void rejectsWhenSignatureInvalid() {
    MockServerHttpRequest request = MockServerHttpRequest.post("/secure")
        .header(HeaderNames.X_TENANT_ID, "tenant-a")
        .header("X-Signature", "deadbeef")
        .body("");
    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    WebFilterChain chain = webExchange -> Mono.empty();

    StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(exchange.getResponse().getBodyAsString().block()).contains("ERR_SIGNATURE_INVALID");
  }

  private String hmac(String payload) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
    StringBuilder sb = new StringBuilder(digest.length * 2);
    for (byte b : digest) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }
}
