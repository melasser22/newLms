package com.ejada.gateway.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.ejada.common.dto.BaseResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ejada.gateway.context.GatewayRequestAttributes;
import java.net.URI;
import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.test.StepVerifier;

class GatewayErrorWebExceptionHandlerTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  private final GatewayErrorWebExceptionHandler handler = new GatewayErrorWebExceptionHandler(
      new StaticObjectProvider<>(objectMapper),
      new StaticObjectProvider<>(null));

  @Test
  void mapsWebClientConnectionFailuresToServiceUnavailable() throws Exception {
    MockServerWebExchange exchange = MockServerWebExchange.from(
        MockServerHttpRequest.get("/test").build());

    WebClientRequestException exception = new WebClientRequestException(
        new java.net.ConnectException("Connection refused"),
        HttpMethod.GET,
        new URI("http://downstream"),
        HttpHeaders.EMPTY);

    StepVerifier.create(handler.handle(exchange, exception)).verifyComplete();

    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    BaseResponse<Map<String, Object>> response = readBody(exchange);
    assertThat(response.getCode()).isEqualTo("ERR_UPSTREAM_UNAVAILABLE");
    assertThat(response.getMessage()).isEqualTo("Upstream service is unavailable");
    assertThat(response.getData())
        .containsEntry("status", HttpStatus.SERVICE_UNAVAILABLE.value())
        .containsEntry("errorCode", "ERR_UPSTREAM_UNAVAILABLE")
        .containsEntry("supportUrl", "https://support.example.com/error/503");
  }

  @Test
  void mapsTimeoutsToGatewayTimeout() throws Exception {
    MockServerWebExchange exchange = MockServerWebExchange.from(
        MockServerHttpRequest.get("/timeout").build());

    WebClientRequestException exception = new WebClientRequestException(
        new TimeoutException("Read timed out"),
        HttpMethod.POST,
        new URI("http://slow-service"),
        HttpHeaders.EMPTY);

    StepVerifier.create(handler.handle(exchange, exception)).verifyComplete();

    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
    BaseResponse<Map<String, Object>> response = readBody(exchange);
    assertThat(response.getCode()).isEqualTo("ERR_UPSTREAM_TIMEOUT");
    assertThat(response.getMessage()).isEqualTo("Upstream service timed out");
    assertThat(response.getData())
        .containsEntry("status", HttpStatus.GATEWAY_TIMEOUT.value())
        .containsEntry("errorCode", "ERR_UPSTREAM_TIMEOUT")
        .containsEntry("supportUrl", "https://support.example.com/error/504");
  }

  @Test
  void mapsUpstreamServerErrorsToBadGateway() throws Exception {
    MockServerWebExchange exchange = MockServerWebExchange.from(
        MockServerHttpRequest.post("/first-login").build());

    WebClientResponseException exception = WebClientResponseException.create(
        HttpStatus.INTERNAL_SERVER_ERROR.value(),
        "Internal Server Error",
        HttpHeaders.EMPTY,
        "{\"message\":\"security down\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8),
        java.nio.charset.StandardCharsets.UTF_8);

    StepVerifier.create(handler.handle(exchange, exception)).verifyComplete();

    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
    BaseResponse<Map<String, Object>> response = readBody(exchange);
    assertThat(response.getCode()).isEqualTo("ERR_UPSTREAM_SERVER_ERROR");
    assertThat(response.getMessage())
        .isEqualTo("Upstream service responded with 500 INTERNAL_SERVER_ERROR: {\"message\":\"security down\"}");
    assertThat(response.getData())
        .containsEntry("status", HttpStatus.BAD_GATEWAY.value())
        .containsEntry("upstreamStatus", HttpStatus.INTERNAL_SERVER_ERROR.value())
        .containsEntry("upstreamStatusText", "Internal Server Error")
        .containsEntry("errorCode", "ERR_UPSTREAM_SERVER_ERROR");
  }

  @Test
  void mapsUpstreamClientErrorsToOriginalStatus() throws Exception {
    MockServerWebExchange exchange = MockServerWebExchange.from(
        MockServerHttpRequest.post("/first-login").build());

    WebClientResponseException exception = WebClientResponseException.create(
        HttpStatus.BAD_REQUEST.value(),
        "Bad Request",
        HttpHeaders.EMPTY,
        "missing fields".getBytes(java.nio.charset.StandardCharsets.UTF_8),
        java.nio.charset.StandardCharsets.UTF_8);

    StepVerifier.create(handler.handle(exchange, exception)).verifyComplete();

    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    BaseResponse<Map<String, Object>> response = readBody(exchange);
    assertThat(response.getCode()).isEqualTo("ERR_UPSTREAM_CLIENT_ERROR");
    assertThat(response.getMessage())
        .isEqualTo("Upstream service responded with 400 BAD_REQUEST: missing fields");
    assertThat(response.getData())
        .containsEntry("status", HttpStatus.BAD_REQUEST.value())
        .containsEntry("upstreamStatus", HttpStatus.BAD_REQUEST.value());
  }

  @Test
  void enrichesFallbackMessageWithCorrelationId() throws Exception {
    MockServerWebExchange exchange = MockServerWebExchange.from(
        MockServerHttpRequest.get("/internal-error").build());
    exchange.getAttributes().put(GatewayRequestAttributes.CORRELATION_ID, "corr-123");
    exchange.getAttributes().put(GatewayRequestAttributes.TENANT_ID, "tenant-42");

    StepVerifier.create(handler.handle(exchange, new RuntimeException("boom"))).verifyComplete();

    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    BaseResponse<Map<String, Object>> response = readBody(exchange);
    assertThat(response.getCode()).isEqualTo("ERR_INTERNAL");
    assertThat(response.getMessage())
        .isEqualTo("An unexpected error occurred. Please contact support with correlation ID corr-123.");
    assertThat(response.getData())
        .containsEntry("correlationId", "corr-123")
        .containsEntry("tenantId", "tenant-42")
        .containsEntry("supportUrl", "https://support.example.com/error/500")
        .containsEntry("path", "/internal-error")
        .containsEntry("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
  }

  @Test
  void mapsSpringCloudNotFoundToNotFound() throws Exception {
    MockServerWebExchange exchange = MockServerWebExchange.from(
        MockServerHttpRequest.get("/missing").build());

    StepVerifier.create(handler.handle(exchange, new NotFoundException("No route defined"))).verifyComplete();

    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    BaseResponse<Map<String, Object>> response = readBody(exchange);
    assertThat(response.getCode()).isEqualTo("ERR_RESOURCE_NOT_FOUND");
    assertThat(response.getData())
        .containsEntry("status", HttpStatus.NOT_FOUND.value())
        .containsEntry("path", "/missing");
  }

  @Test
  void mapsAuthenticationFailuresToUnauthorized() throws Exception {
    MockServerWebExchange exchange = MockServerWebExchange.from(
        MockServerHttpRequest.post("/secure").build());

    AuthenticationServiceException exception = new AuthenticationServiceException("Invalid credentials");

    StepVerifier.create(handler.handle(exchange, exception)).verifyComplete();

    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    BaseResponse<Map<String, Object>> response = readBody(exchange);
    assertThat(response.getCode()).isEqualTo("ERR_AUTHENTICATION");
    assertThat(response.getMessage()).isEqualTo("Invalid credentials");
    assertThat(response.getData())
        .containsEntry("status", HttpStatus.UNAUTHORIZED.value())
        .containsEntry("errorCode", "ERR_AUTHENTICATION");
  }

  @Test
  void unwrapsAuthenticationFailureFromNestedException() throws Exception {
    MockServerWebExchange exchange = MockServerWebExchange.from(
        MockServerHttpRequest.post("/secure").build());

    RuntimeException wrapper = new RuntimeException("Wrapper",
        new AuthenticationServiceException("Token introspection unavailable"));

    StepVerifier.create(handler.handle(exchange, wrapper)).verifyComplete();

    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    BaseResponse<Map<String, Object>> response = readBody(exchange);
    assertThat(response.getCode()).isEqualTo("ERR_AUTHENTICATION");
    assertThat(response.getMessage()).isEqualTo("Token introspection unavailable");
    assertThat(response.getData())
        .containsEntry("status", HttpStatus.UNAUTHORIZED.value())
        .containsEntry("errorCode", "ERR_AUTHENTICATION");
  }

  private BaseResponse<Map<String, Object>> readBody(MockServerWebExchange exchange) throws Exception {
    String body = exchange.getResponse()
        .getBodyAsString()
        .block(Duration.ofSeconds(1));
    assertThat(body).isNotBlank();
    return objectMapper.readValue(body, new TypeReference<BaseResponse<Map<String, Object>>>() {});
  }

  private static final class StaticObjectProvider<T> implements ObjectProvider<T> {

    @Nullable
    private final T value;

    private StaticObjectProvider(@Nullable T value) {
      this.value = value;
    }

    @Override
    public T getObject(Object... args) {
      return Objects.requireNonNull(value);
    }

    @Override
    public T getObject() {
      return Objects.requireNonNull(value);
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
    public Stream<T> stream() {
      return value != null ? Stream.of(value) : Stream.empty();
    }

    @Override
    public Stream<T> orderedStream() {
      return stream();
    }

    @Override
    public Iterator<T> iterator() {
      return stream().iterator();
    }

  }
}

