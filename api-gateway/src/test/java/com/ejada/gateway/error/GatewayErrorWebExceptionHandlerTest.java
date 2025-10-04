package com.ejada.gateway.error;

import com.ejada.common.exception.ValidationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GatewayErrorWebExceptionHandlerTest {

  private ObjectMapper objectMapper;
  private GatewayErrorWebExceptionHandler handler;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    objectMapper.findAndRegisterModules();
    handler = new GatewayErrorWebExceptionHandler(objectMapper, new StaticObjectProvider<>(objectMapper));
  }

  @Test
  void convertsValidationExceptionsToBadRequestResponses() throws Exception {
    MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/test").build());

    StepVerifier.create(handler.handle(exchange, new ValidationException("Email is required")))
        .verifyComplete();

    assertEquals(HttpStatus.BAD_REQUEST, exchange.getResponse().getStatusCode());
    JsonNode payload = readResponse(exchange);
    assertEquals("ERR_VALIDATION", payload.get("code").asText());
    assertEquals("Email is required", payload.get("message").asText());
    assertEquals("ERROR", payload.get("status").asText());
  }

  @Test
  void masksUnexpectedExceptionsWithGenericMessage() throws Exception {
    MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/error").build());

    StepVerifier.create(handler.handle(exchange, new RuntimeException("sensitive stack trace")))
        .verifyComplete();

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exchange.getResponse().getStatusCode());
    JsonNode payload = readResponse(exchange);
    assertEquals("ERR_INTERNAL", payload.get("code").asText());
    assertEquals("An unexpected error occurred", payload.get("message").asText());
  }

  @Test
  void preservesResponseStatusExceptionReason() throws Exception {
    MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/status").build());

    ResponseStatusException exception = new ResponseStatusException(HttpStatus.NOT_FOUND, "No route found");

    StepVerifier.create(handler.handle(exchange, exception)).verifyComplete();

    assertEquals(HttpStatus.NOT_FOUND, exchange.getResponse().getStatusCode());
    JsonNode payload = readResponse(exchange);
    assertEquals("ERR_RESOURCE_NOT_FOUND", payload.get("code").asText());
    assertEquals("No route found", payload.get("message").asText());
  }

  private JsonNode readResponse(MockServerWebExchange exchange) throws Exception {
    String body = exchange.getResponse().getBodyAsString().block();
    assertNotNull(body);
    return objectMapper.readTree(body);
  }

  private static final class StaticObjectProvider<T> implements ObjectProvider<T> {

    private final T instance;

    private StaticObjectProvider(T instance) {
      this.instance = instance;
    }

    @Override
    public T getObject(Object... args) {
      return instance;
    }

    @Override
    public T getObject() {
      return instance;
    }

    @Override
    public T getIfAvailable() {
      return instance;
    }

    @Override
    public T getIfAvailable(Supplier<T> supplier) {
      return instance != null ? instance : supplier.get();
    }

    @Override
    public T getIfUnique() {
      return instance;
    }

    @Override
    public T getIfUnique(Supplier<T> supplier) {
      return instance != null ? instance : supplier.get();
    }

    @Override
    public Stream<T> stream() {
      return instance != null ? Stream.of(instance) : Stream.empty();
    }

    @Override
    public Stream<T> orderedStream() {
      return stream();
    }

    @Override
    public void forEach(Consumer<? super T> action) {
      if (instance != null) {
        action.accept(instance);
      }
    }

    @Override
    public Iterator<T> iterator() {
      return stream().iterator();
    }
  }
}
