package com.ejada.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.ejada.common.constants.HeaderNames;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;
import reactor.test.StepVerifier;

class GatewayNotFoundHandlerTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private GenericApplicationContext applicationContext;
  private GatewayNotFoundHandler handler;

  @BeforeEach
  void setUp() {
    applicationContext = new GenericApplicationContext();
    applicationContext.refresh();

    ErrorAttributes errorAttributes = new DefaultErrorAttributes();
    WebProperties webProperties = new WebProperties();
    ServerCodecConfigurer codecConfigurer = ServerCodecConfigurer.create();
    ObjectProvider<ObjectMapper> provider = new StaticObjectProvider<>(objectMapper);

    handler = new GatewayNotFoundHandler(
        errorAttributes,
        webProperties,
        applicationContext,
        codecConfigurer,
        provider,
        provider);
  }

  @AfterEach
  void tearDown() {
    applicationContext.close();
  }

  @Test
  void rendersResponseForSpringCloudNotFoundException() throws Exception {
    MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/missing")
        .header(HeaderNames.CORRELATION_ID, "corr-404")
        .header(HeaderNames.X_TENANT_ID, "tenant-404")
        .build());

    StepVerifier.create(handler.handle(exchange, new NotFoundException("No route defined for GET /missing")))
        .verifyComplete();

    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    Map<String, Object> response = readBody(exchange);
    assertThat(response)
        .containsEntry("status", HttpStatus.NOT_FOUND.value())
        .containsEntry("error", "Not Found")
        .containsEntry("path", "/missing")
        .containsEntry("method", "GET")
        .containsEntry("correlationId", "corr-404")
        .containsEntry("tenantId", "tenant-404");
    assertThat(response.get("message").toString()).contains("/missing");
  }

  @Test
  void delegatesNonNotFoundErrors() {
    MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/missing").build());
    ResponseStatusException exception = new ResponseStatusException(HttpStatus.BAD_REQUEST);

    StepVerifier.create(handler.handle(exchange, exception))
        .expectErrorSatisfies(thrown -> assertThat(thrown).isSameAs(exception))
        .verify();
  }

  private Map<String, Object> readBody(MockServerWebExchange exchange) throws Exception {
    String body = exchange.getResponse().getBodyAsString().block(Duration.ofSeconds(1));
    return objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
  }

  private static final class StaticObjectProvider<T> implements ObjectProvider<T> {

    private final T value;

    private StaticObjectProvider(T value) {
      this.value = value;
    }

    @Override
    public T getObject(Object... args) {
      return value;
    }

    @Override
    public T getObject() {
      return value;
    }

    @Override
    public T getIfAvailable() {
      return value;
    }

    @Override
    public T getIfAvailable(java.util.function.Supplier<T> supplier) {
      return value != null ? value : supplier.get();
    }

    @Override
    public T getIfUnique() {
      return value;
    }

    @Override
    public T getIfUnique(java.util.function.Supplier<T> supplier) {
      return value != null ? value : supplier.get();
    }

    @Override
    public java.util.stream.Stream<T> stream() {
      return value != null ? java.util.stream.Stream.of(value) : java.util.stream.Stream.empty();
    }

    @Override
    public java.util.stream.Stream<T> orderedStream() {
      return stream();
    }

    @Override
    public java.util.Iterator<T> iterator() {
      return stream().iterator();
    }
  }
}
