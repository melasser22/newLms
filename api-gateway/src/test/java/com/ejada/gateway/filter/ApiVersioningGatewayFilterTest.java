package com.ejada.gateway.filter;

import com.ejada.common.constants.HeaderNames;
import com.ejada.gateway.config.GatewayRoutesProperties;
import com.ejada.gateway.context.GatewayRequestAttributes;
import java.net.URI;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiVersioningGatewayFilterTest {

  @Test
  void stripsVersionPrefixAndPropagatesHeader() {
    GatewayRoutesProperties.ServiceRoute route = buildRoute(List.of("/demo/**"));
    GatewayRoutesProperties.ServiceRoute.Versioning versioning = route.getVersioning();
    versioning.setEnabled(true);
    versioning.setDefaultVersion("v1");
    versioning.setSupportedVersions(List.of("v1", "v2"));
    route.validate("demo");

    ApiVersioningGatewayFilter filter = new ApiVersioningGatewayFilter(versioning);

    MockServerWebExchange exchange = MockServerWebExchange.from(
        MockServerHttpRequest.get("/v2/demo/items").build());

    AtomicReference<String> path = new AtomicReference<>();
    AtomicReference<String> header = new AtomicReference<>();
    AtomicReference<String> attribute = new AtomicReference<>();

    GatewayFilterChain chain = webExchange -> {
      path.set(webExchange.getRequest().getURI().getRawPath());
      header.set(webExchange.getRequest().getHeaders().getFirst(HeaderNames.API_VERSION));
      attribute.set((String) webExchange.getAttribute(GatewayRequestAttributes.API_VERSION));
      return Mono.empty();
    };

    filter.filter(exchange, chain).block();

    assertEquals("/demo/items", path.get());
    assertEquals("v2", header.get());
    assertEquals("v2", attribute.get());
  }

  @Test
  void assignsDefaultVersionWhenMissing() {
    GatewayRoutesProperties.ServiceRoute route = buildRoute(List.of("/demo/**"));
    GatewayRoutesProperties.ServiceRoute.Versioning versioning = route.getVersioning();
    versioning.setEnabled(true);
    versioning.setDefaultVersion("v3");
    route.validate("demo");

    ApiVersioningGatewayFilter filter = new ApiVersioningGatewayFilter(versioning);

    MockServerWebExchange exchange = MockServerWebExchange.from(
        MockServerHttpRequest.get("/demo/items").build());

    AtomicReference<String> header = new AtomicReference<>();
    AtomicReference<String> attribute = new AtomicReference<>();

    GatewayFilterChain chain = webExchange -> {
      header.set(webExchange.getRequest().getHeaders().getFirst(HeaderNames.API_VERSION));
      attribute.set((String) webExchange.getAttribute(GatewayRequestAttributes.API_VERSION));
      return Mono.empty();
    };

    filter.filter(exchange, chain).block();

    assertEquals("/demo/items", exchange.getRequest().getURI().getRawPath());
    assertEquals("v3", header.get());
    assertEquals("v3", attribute.get());
  }

  @Test
  void rejectsUnsupportedVersionWhenFallbackDisabled() {
    GatewayRoutesProperties.ServiceRoute route = buildRoute(List.of("/demo/**"));
    GatewayRoutesProperties.ServiceRoute.Versioning versioning = route.getVersioning();
    versioning.setEnabled(true);
    versioning.setDefaultVersion("v1");
    versioning.setSupportedVersions(List.of("v1"));
    versioning.setFallbackToDefault(false);
    route.validate("demo");

    ApiVersioningGatewayFilter filter = new ApiVersioningGatewayFilter(versioning);

    MockServerWebExchange exchange = MockServerWebExchange.from(
        MockServerHttpRequest.get("/v9/demo/items").build());

    GatewayFilterChain chain = webExchange -> Mono.empty();

    StepVerifier.create(filter.filter(exchange, chain))
        .expectErrorSatisfies(throwable -> {
          assertEquals(ResponseStatusException.class, throwable.getClass());
          ResponseStatusException exception = (ResponseStatusException) throwable;
          assertEquals(404, exception.getStatusCode().value());
          assertEquals("Requested API version is not supported", exception.getReason());
        })
        .verify();
  }

  private GatewayRoutesProperties.ServiceRoute buildRoute(List<String> paths) {
    GatewayRoutesProperties.ServiceRoute route = new GatewayRoutesProperties.ServiceRoute();
    route.setId("demo");
    route.setUri(URI.create("http://example.com"));
    route.setPaths(paths);
    return route;
  }
}
