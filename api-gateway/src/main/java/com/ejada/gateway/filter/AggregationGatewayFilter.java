package com.ejada.gateway.filter;

import com.ejada.gateway.config.GatewayAggregationProperties;
import com.ejada.gateway.config.GatewayAggregationProperties.AggregationRoute;
import com.ejada.gateway.config.GatewayAggregationProperties.UpstreamRequest;
import com.ejada.gateway.context.GatewayRequestAttributes;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Gateway filter that aggregates multiple downstream calls in parallel and returns a combined JSON
 * payload to the caller. Configured via {@link GatewayAggregationProperties} on a per-route basis.
 */
@Component
public class AggregationGatewayFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(AggregationGatewayFilter.class);

  private static final String METRIC_DURATION = "gateway.aggregation.duration";
  private static final String METRIC_OUTCOME = "gateway.aggregation.outcome";

  private final GatewayAggregationProperties properties;
  private final WebClient.Builder webClientBuilder;
  private final ObjectMapper objectMapper;
  private final ReactiveCircuitBreakerFactory<?, ?> circuitBreakerFactory;
  private final MeterRegistry meterRegistry;

  private final Map<String, AggregationRoute> validatedRoutes = new ConcurrentHashMap<>();

  public AggregationGatewayFilter(GatewayAggregationProperties properties,
      WebClient.Builder webClientBuilder,
      ObjectMapper objectMapper,
      ObjectProvider<ReactiveCircuitBreakerFactory<?, ?>> circuitBreakerFactoryProvider,
      ObjectProvider<MeterRegistry> meterRegistryProvider) {
    this.properties = Objects.requireNonNull(properties, "properties");
    this.webClientBuilder = Objects.requireNonNull(webClientBuilder, "webClientBuilder");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    this.circuitBreakerFactory = circuitBreakerFactoryProvider.getIfAvailable();
    this.meterRegistry = meterRegistryProvider.getIfAvailable();
  }

  public GatewayFilter apply(String routeId) {
    return new RouteAggregationFilter(routeId);
  }

  private AggregationRoute resolveRouteConfig(String routeId) {
    if (!properties.isEnabled()) {
      return null;
    }
    AggregationRoute cached = validatedRoutes.get(routeId);
    if (cached != null) {
      return cached;
    }
    Optional<AggregationRoute> optional = properties.findRoute(routeId);
    if (optional.isEmpty()) {
      return null;
    }
    AggregationRoute route = optional.get();
    route.validate(routeId);
    validatedRoutes.put(routeId, route);
    return route;
  }

  private class RouteAggregationFilter implements GatewayFilter {

    private final String routeId;

    RouteAggregationFilter(String routeId) {
      this.routeId = routeId;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
      AggregationRoute routeConfig = resolveRouteConfig(routeId);
      if (routeConfig == null) {
        return chain.filter(exchange);
      }

      Timer.Sample sample = startTimer(routeId);

      return Flux.fromIterable(routeConfig.getUpstreamRequests())
          .flatMap(upstream -> executeRequest(routeId, routeConfig, upstream, exchange))
          .collectList()
          .flatMap(results -> renderResponse(exchange, routeConfig, results))
          .doOnSuccess(unused -> recordOutcome(routeId, "success", sample))
          .doOnError(ex -> recordOutcome(routeId, "error", sample))
          .onErrorResume(ex -> handleFailure(exchange, ex));
    }
  }

  private Mono<Void> renderResponse(ServerWebExchange exchange,
      AggregationRoute routeConfig,
      List<AggregationResult> results) {
    ObjectNode root = objectMapper.createObjectNode();
    ObjectNode data = objectMapper.createObjectNode();
    ObjectNode errors = objectMapper.createObjectNode();

    boolean hasErrors = false;
    for (AggregationResult result : results) {
      if (result.value() != null) {
        data.set(result.id(), result.value());
      } else {
        hasErrors = true;
        String message = routeConfig.isIncludeErrorDetails() && result.error() != null
            ? sanitizeErrorMessage(result.error())
            : "Unavailable";
        errors.put(result.id(), message);
      }
    }

    root.set("data", data);
    if (hasErrors) {
      root.set("errors", errors);
    }

    try {
      byte[] payload = objectMapper.writeValueAsBytes(root);
      exchange.getResponse().setStatusCode(HttpStatus.OK);
      HttpHeaders headers = exchange.getResponse().getHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(payload);
      return exchange.getResponse().writeWith(Mono.just(buffer));
    } catch (Exception ex) {
      return handleFailure(exchange, ex);
    }
  }

  private Mono<Void> handleFailure(ServerWebExchange exchange, Throwable throwable) {
    LOGGER.warn("Aggregation failed", throwable);
    ObjectNode error = objectMapper.createObjectNode();
    error.put("status", "ERROR");
    error.put("message", "Aggregation failed");
    if (LOGGER.isDebugEnabled()) {
      error.put("details", sanitizeErrorMessage(throwable));
    }
    byte[] bytes = error.toString().getBytes(StandardCharsets.UTF_8);
    exchange.getResponse().setStatusCode(HttpStatus.BAD_GATEWAY);
    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
    DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
    return exchange.getResponse().writeWith(Mono.just(buffer));
  }

  private String sanitizeErrorMessage(Throwable throwable) {
    String message = throwable.getMessage();
    if (message == null) {
      return throwable.getClass().getSimpleName();
    }
    return message.replaceAll("\n", " ").trim();
  }

  private Mono<AggregationResult> executeRequest(String routeId,
      AggregationRoute routeConfig,
      UpstreamRequest upstream,
      ServerWebExchange exchange) {
    Duration timeout = upstream.resolveTimeout(routeConfig.getTimeout());
    String targetUri = resolveTargetUri(exchange, upstream);
    WebClient.RequestBodySpec requestSpec = webClientBuilder.clone()
        .build()
        .method(upstream.getMethod())
        .uri(targetUri);

    if (!CollectionUtils.isEmpty(upstream.getHeaders())) {
      requestSpec.headers(httpHeaders -> upstream.getHeaders().forEach(httpHeaders::add));
    }

    Mono<ClientResponse> responseMono = requestSpec.exchangeToMono(Mono::just);
    if (circuitBreakerFactory != null && StringUtils.hasText(upstream.getCircuitBreakerName())) {
      ReactiveCircuitBreaker circuitBreaker = circuitBreakerFactory.create(upstream.getCircuitBreakerName());
      responseMono = Mono.defer(() -> circuitBreaker.run(responseMono, Mono::error));
    }

    return responseMono
        .flatMap(clientResponse -> clientResponse.bodyToMono(JsonNode.class)
            .timeout(timeout)
            .map(body -> new AggregationResult(upstream.getId(), body, null)))
        .timeout(timeout)
        .onErrorResume(ex -> {
          LOGGER.warn("Aggregation request {} for route {} failed: {}", upstream.getId(), routeId, ex.toString());
          recordTargetFailure(routeId, upstream.getId());
          if (upstream.isOptional()) {
            return Mono.just(new AggregationResult(upstream.getId(), null, ex));
          }
          return Mono.just(new AggregationResult(upstream.getId(), null, ex));
        });
  }

  private Timer.Sample startTimer(String routeId) {
    if (meterRegistry == null) {
      return null;
    }
    return Timer.start(meterRegistry);
  }

  private void recordOutcome(String routeId, String outcome, Timer.Sample sample) {
    if (meterRegistry != null) {
      Counter.builder(METRIC_OUTCOME)
          .tag("route", routeId)
          .tag("outcome", outcome)
          .register(meterRegistry)
          .increment();
      if (sample != null) {
        sample.stop(Timer.builder(METRIC_DURATION)
            .tag("route", routeId)
            .tag("outcome", outcome)
            .register(meterRegistry));
      }
    }
  }

  private void recordTargetFailure(String routeId, String targetId) {
    if (meterRegistry != null) {
      Counter.builder("gateway.aggregation.target.failures")
          .tag("route", routeId)
          .tag("target", targetId)
          .register(meterRegistry)
          .increment();
    }
  }

  private String resolveTargetUri(ServerWebExchange exchange, UpstreamRequest upstream) {
    String template = upstream.getUriTemplate();
    if (!StringUtils.hasText(template)) {
      URI uri = upstream.getUri();
      return (uri != null) ? uri.toString() : "";
    }
    Map<String, Object> variables = new HashMap<>();
    Map<String, String> pathVariables = exchange.getAttributeOrDefault(
        ServerWebExchangeUtils.URI_TEMPLATE_VARIABLES_ATTRIBUTE,
        Collections.emptyMap());
    variables.putAll(pathVariables);
    exchange.getRequest().getQueryParams().forEach((key, values) -> {
      if (!values.isEmpty() && !variables.containsKey(key)) {
        variables.put(key, values.get(0));
      }
    });
    Object tenant = exchange.getAttribute(GatewayRequestAttributes.TENANT_ID);
    if (tenant != null && !variables.containsKey("tenantId")) {
      variables.put("tenantId", tenant);
    }
    try {
      return new UriTemplate(template).expand(variables).toString();
    } catch (IllegalArgumentException ex) {
      LOGGER.warn("Failed to expand aggregation URI template {}: {}", template, ex.toString());
      return template;
    }
  }

  private record AggregationResult(String id, JsonNode value, Throwable error) {
  }
}

