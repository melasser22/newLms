package com.ejada.gateway.admin;

import com.ejada.common.constants.HeaderNames;
import com.ejada.common.context.ContextManager;
import com.ejada.gateway.admin.model.AdminOverview;
import com.ejada.gateway.admin.model.AdminRouteView;
import com.ejada.gateway.admin.model.AdminServiceSnapshot;
import com.ejada.gateway.admin.model.AdminServiceState;
import com.ejada.gateway.admin.model.DetailedHealthStatus;
import com.ejada.gateway.admin.model.DetailedHealthStatus.CircuitBreakerHealth;
import com.ejada.gateway.admin.model.DetailedHealthStatus.RedisHealthStatus;
import com.ejada.gateway.config.AdminAggregationProperties;
import com.ejada.gateway.config.GatewayRoutesProperties;
import com.ejada.gateway.loadbalancer.LoadBalancerHealthCheckAggregator;
import com.ejada.gateway.loadbalancer.LoadBalancerHealthCheckAggregator.Availability;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.annotation.Timed;
import io.netty.channel.ConnectTimeoutException;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.WriteTimeoutException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.channels.UnresolvedAddressException;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.Exceptions;

/**
 * Aggregates status information across downstream services so platform operators can inspect the
 * health of the estate via the admin API hosted by the gateway.
 */
@Service
public class AdminAggregationService {

  private static final Logger LOGGER = LoggerFactory.getLogger(AdminAggregationService.class);

  private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
      new ParameterizedTypeReference<>() {};

  private final WebClient.Builder webClientBuilder;
  private final AdminAggregationProperties adminProperties;
  private final GatewayRoutesProperties routesProperties;
  private final ReactiveStringRedisTemplate redisTemplate;
  private final CircuitBreakerRegistry circuitBreakerRegistry;
  private final LoadBalancerHealthCheckAggregator loadBalancerAggregator;

  public AdminAggregationService(WebClient.Builder webClientBuilder,
      AdminAggregationProperties adminProperties,
      GatewayRoutesProperties routesProperties,
      ObjectProvider<ReactiveStringRedisTemplate> redisTemplateProvider,
      ObjectProvider<CircuitBreakerRegistry> circuitBreakerRegistryProvider,
      ObjectProvider<LoadBalancerHealthCheckAggregator> loadBalancerAggregatorProvider) {
    this.webClientBuilder = Objects.requireNonNull(webClientBuilder, "webClientBuilder");
    this.adminProperties = adminProperties;
    this.routesProperties = routesProperties;
    this.redisTemplate = redisTemplateProvider.getIfAvailable();
    this.circuitBreakerRegistry = circuitBreakerRegistryProvider.getIfAvailable();
    this.loadBalancerAggregator = loadBalancerAggregatorProvider.getIfAvailable();
  }

  @Timed(value = "gateway.admin.overview", description = "Aggregated overview retrieval")
  public Mono<AdminOverview> fetchOverview() {
    return collectDownstreamSnapshots().map(AdminOverview::fromSnapshots);
  }

  @Timed(value = "gateway.admin.routes", description = "Admin route catalogue generation")
  public List<AdminRouteView> describeRoutes() {
    return routesProperties.getRoutes().values().stream()
        .sorted(Comparator.comparing(route -> {
          String id = route.getId();
          return id == null ? "" : id;
        }))
        .map(AdminRouteView::fromRoute)
        .toList();
  }

  @Timed(value = "gateway.admin.loadbalancer.health", description = "Load balancer health snapshot")
  public List<LoadBalancerHealthCheckAggregator.InstanceState> collectLoadBalancerHealth(String serviceId) {
    if (loadBalancerAggregator == null) {
      return List.of();
    }
    return loadBalancerAggregator.snapshot(serviceId);
  }

  @Timed(value = "gateway.admin.downstream.snapshots", description = "Downstream service snapshot aggregation")
  public Mono<List<AdminServiceSnapshot>> collectDownstreamSnapshots() {
    return Mono.defer(() -> {
      List<AdminAggregationProperties.Service> services = adminProperties.getAggregation().getServices();
      if (services.isEmpty()) {
        return Mono.just(List.of());
      }

      try {
        IntStream.range(0, services.size()).forEach(index -> {
          AdminAggregationProperties.Service service = services.get(index);
          String key = StringUtils.hasText(service.getId())
              ? service.getId()
              : String.valueOf(index);
          service.validate(key);
        });
      } catch (RuntimeException ex) {
        return Mono.error(ex);
      }

      Duration timeout = adminProperties.getAggregation().getTimeout();

      return Flux.fromIterable(services)
          .flatMap(service -> fetchSnapshot(service, timeout))
          .sort(Comparator.comparing(AdminServiceSnapshot::serviceId))
          .collectList();
    });
  }

  @Timed(value = "gateway.admin.health.detailed", description = "Detailed gateway health aggregation")
  public Mono<DetailedHealthStatus> fetchDetailedHealth() {
    return Mono.defer(() -> {
      Mono<RedisHealthStatus> redisHealth = checkRedisHealth();
      Mono<List<AdminServiceSnapshot>> downstream = collectDownstreamSnapshots();
      Mono<List<CircuitBreakerHealth>> circuitBreakers = Mono.fromSupplier(this::collectCircuitBreakerStates);

      return Mono.zip(redisHealth, downstream, circuitBreakers)
          .map(tuple -> new DetailedHealthStatus(tuple.getT1(), tuple.getT2(), tuple.getT3()))
          .defaultIfEmpty(DetailedHealthStatus.empty());
    });
  }

  @Timed(value = "gateway.admin.health.redis", description = "Redis connectivity check")
  public Mono<RedisHealthStatus> checkRedisHealth() {
    if (redisTemplate == null) {
      return Mono.just(RedisHealthStatus.unavailable());
    }
    Duration timeout = adminProperties.getAggregation().getTimeout();
    String probeKey = adminProperties.getAggregation().getRedisProbeKey();
    String key = (probeKey != null && !probeKey.isBlank()) ? probeKey : "gateway:health:probe";
    return Mono.defer(() -> {
          Instant start = Instant.now();
          return redisTemplate.hasKey(key)
              .timeout(timeout)
              .map(result -> new RedisHealthStatus("UP",
                  Duration.between(start, Instant.now()).toMillis(),
                  Boolean.TRUE.equals(result) ? "KeyExists" : "Reachable"));
        })
        .onErrorResume(ex -> Mono.just(new RedisHealthStatus("DOWN", -1,
            ex.getClass().getSimpleName() + ": " + ex.getMessage())));
  }

  @Timed(value = "gateway.admin.health.circuitbreakers", description = "Circuit breaker state capture")
  public List<CircuitBreakerHealth> collectCircuitBreakerStates() {
    if (circuitBreakerRegistry == null) {
      return List.of();
    }
    return circuitBreakerRegistry.getAllCircuitBreakers().stream()
        .map(cb -> new CircuitBreakerHealth(cb.getName(), cb.getState().name()))
        .sorted(Comparator.comparing(CircuitBreakerHealth::serviceName))
        .toList();
  }

  private Mono<AdminServiceSnapshot> fetchSnapshot(AdminAggregationProperties.Service service,
      Duration defaultTimeout) {
    Optional<AdminServiceSnapshot> shortCircuit = shortCircuitIfNoInstances(service);
    if (shortCircuit.isPresent()) {
      return Mono.just(shortCircuit.get());
    }

    WebClient client = webClientBuilder.clone()
        .baseUrl(service.getUri().toString())
        .build();
    Duration timeout = service.resolveTimeout(defaultTimeout);
    String correlationId = StringUtils.trimWhitespace(ContextManager.getCorrelationId());
    String tenantId = StringUtils.trimWhitespace(ContextManager.Tenant.get());

    return client.get()
        .uri(service.getHealthPath())
        .headers(httpHeaders -> {
          service.getHeaders().forEach(httpHeaders::add);
          if (StringUtils.hasText(correlationId)
              && !httpHeaders.containsKey(HeaderNames.CORRELATION_ID)) {
            httpHeaders.set(HeaderNames.CORRELATION_ID, correlationId);
          }
          if (StringUtils.hasText(tenantId)
              && !httpHeaders.containsKey(HeaderNames.X_TENANT_ID)) {
            httpHeaders.set(HeaderNames.X_TENANT_ID, tenantId);
          }
        })
        .retrieve()
        .bodyToMono(MAP_TYPE)
        .timeout(timeout)
        .elapsed()
        .map(tuple -> AdminServiceSnapshot.success(
            service.getId(),
            service.getDeployment(),
            service.isRequired(),
            tuple.getT2(),
            tuple.getT1(),
            Instant.now()))
        .onErrorResume(ex -> {
          logAggregationFailure(service, ex);
          AdminServiceState state = classifyFailure(ex);
          String failureStatus = mapFailureStatus(state);
          Instant timestamp = Instant.now();
          if (ex instanceof WebClientResponseException httpEx) {
            HttpStatusCode statusCode = httpEx.getStatusCode();
            if (statusCode.isSameCodeAs(HttpStatus.UNAUTHORIZED)
                || statusCode.isSameCodeAs(HttpStatus.FORBIDDEN)) {
              return Mono.just(AdminServiceSnapshot.unauthorized(
                  service.getId(),
                  service.getDeployment(),
                  service.isRequired(),
                  httpEx,
                  timestamp));
            }
            return Mono.just(AdminServiceSnapshot.httpFailure(
                service.getId(),
                service.getDeployment(),
                service.isRequired(),
                httpEx,
                timestamp));
          }
          return Mono.just(AdminServiceSnapshot.failure(
              service.getId(),
              service.getDeployment(),
              service.isRequired(),
              state,
              failureStatus,
              ex,
              timestamp));
        });
  }

  private Optional<AdminServiceSnapshot> shortCircuitIfNoInstances(AdminAggregationProperties.Service service) {
    if (service.isRequired() || loadBalancerAggregator == null) {
      return Optional.empty();
    }

    List<LoadBalancerHealthCheckAggregator.InstanceState> instances =
        loadBalancerAggregator.snapshot(service.getId());
    boolean hasReachableInstance = instances.stream()
        .anyMatch(state -> state.getAvailability() != Availability.DOWN);
    if (!instances.isEmpty() && hasReachableInstance) {
      return Optional.empty();
    }

    IllegalStateException failure = new IllegalStateException("No service instances discovered");
    logAggregationFailure(service, failure);
    return Optional.of(AdminServiceSnapshot.failure(
        service.getId(),
        service.getDeployment(),
        service.isRequired(),
        failure,
        Instant.now()));
  }

  private void logAggregationFailure(AdminAggregationProperties.Service service, Throwable failure) {
    String serviceId = service.getId();
    String deployment = service.getDeployment();
    String endpoint = describeEndpoint(service);
    String failureMessage = Optional.ofNullable(failure.getMessage())
        .filter(StringUtils::hasText)
        .orElse(failure.getClass().getSimpleName());
    String message = "Admin aggregation failed for {} (deployment: {}) -> {}";
    if (service.isRequired()) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.warn(message, serviceId, deployment, endpoint, failure);
      } else {
        LOGGER.warn(message + ": {}", serviceId, deployment, endpoint, failureMessage);
      }
      return;
    }

    String optionalMessage = "Optional " + message;
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(optionalMessage, serviceId, deployment, endpoint, failure);
    } else {
      LOGGER.info(optionalMessage + ": {}", serviceId, deployment, endpoint, failureMessage);
    }
  }

  private AdminServiceState classifyFailure(Throwable failure) {
    Throwable unwrapped = Exceptions.unwrap(failure);
    if (unwrapped instanceof WebClientResponseException response) {
      if (response.getStatusCode().is5xxServerError()) {
        return AdminServiceState.DOWN;
      }
      if (response.getStatusCode().is4xxClientError()) {
        return AdminServiceState.DEGRADED;
      }
      return AdminServiceState.UNKNOWN;
    }
    if (unwrapped instanceof WebClientRequestException requestException) {
      Throwable cause = requestException.getCause();
      return classifyTransportFailure(cause);
    }
    return classifyTransportFailure(unwrapped);
  }

  private String mapFailureStatus(AdminServiceState state) {
    return switch (state) {
      case DOWN -> "UNAVAILABLE";
      case DEGRADED -> "DEGRADED";
      case UNKNOWN -> "UNKNOWN";
      default -> "UNKNOWN";
    };
  }

  private AdminServiceState classifyTransportFailure(Throwable failure) {
    if (failure == null) {
      return AdminServiceState.UNKNOWN;
    }
    Throwable root = Exceptions.unwrap(failure);
    if (root instanceof SocketTimeoutException
        || root instanceof TimeoutException
        || root instanceof ReadTimeoutException
        || root instanceof WriteTimeoutException) {
      return AdminServiceState.DEGRADED;
    }
    if (root instanceof ConnectException
        || root instanceof ConnectTimeoutException
        || root instanceof NoRouteToHostException
        || root instanceof UnknownHostException
        || root instanceof UnresolvedAddressException
        || root instanceof SocketException) {
      return AdminServiceState.DOWN;
    }
    return AdminServiceState.UNKNOWN;
  }

  private String describeEndpoint(AdminAggregationProperties.Service service) {
    URI uri = service.getUri();
    String healthPath = service.getHealthPath();
    if (uri == null) {
      return healthPath;
    }
    try {
      return uri.resolve(healthPath).toString();
    } catch (IllegalArgumentException ex) {
      return uri + healthPath;
    }
  }
}
