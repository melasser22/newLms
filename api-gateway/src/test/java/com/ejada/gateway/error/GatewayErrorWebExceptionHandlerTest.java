package com.ejada.gateway.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.ejada.common.dto.BaseResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Duration;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.test.StepVerifier;

class GatewayErrorWebExceptionHandlerTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  private final GatewayErrorWebExceptionHandler handler = new GatewayErrorWebExceptionHandler(
      objectMapper,
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
    BaseResponse<Void> response = readBody(exchange);
    assertThat(response.getCode()).isEqualTo("ERR_UPSTREAM_UNAVAILABLE");
    assertThat(response.getMessage()).isEqualTo("Upstream service is unavailable");
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
    BaseResponse<Void> response = readBody(exchange);
    assertThat(response.getCode()).isEqualTo("ERR_UPSTREAM_TIMEOUT");
    assertThat(response.getMessage()).isEqualTo("Upstream service timed out");
  }

  private BaseResponse<Void> readBody(MockServerWebExchange exchange) throws Exception {
    String body = exchange.getResponse()
        .getBodyAsString()
        .block(Duration.ofSeconds(1));
    assertThat(body).isNotBlank();
    return objectMapper.readValue(body, new TypeReference<BaseResponse<Void>>() {});
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

