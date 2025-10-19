package com.ejada.gateway.admin;

import com.ejada.common.constants.HeaderNames;
import com.ejada.common.context.ContextManager;
import com.ejada.gateway.admin.model.AdminOverview;
import com.ejada.gateway.admin.model.AdminRouteView;
import com.ejada.gateway.admin.model.AdminServiceSnapshot;
import com.ejada.gateway.admin.model.DetailedHealthStatus;
import com.ejada.gateway.admin.model.DetailedHealthStatus.CircuitBreakerHealth;
import com.ejada.gateway.admin.model.DetailedHealthStatus.RedisHealthStatus;
import com.ejada.gateway.config.AdminAggregationProperties;
import com.ejada.gateway.config.GatewayRoutesProperties;
import com.ejada.gateway.loadbalancer.LoadBalancerHealthCheckAggregator;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.annotation.Timed;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
          return Mono.just(AdminServiceSnapshot.failure(
              service.getId(),
              service.getDeployment(),
              service.isRequired(),
              ex,
              Instant.now()));
        });
  }

  private void logAggregationFailure(AdminAggregationProperties.Service service, Throwable failure) {
    String serviceId = service.getId();
    String deployment = service.getDeployment();
    String failureMessage = Optional.ofNullable(failure.getMessage())
        .filter(StringUtils::hasText)
        .orElse(failure.getClass().getSimpleName());
    String message = "Admin aggregation failed for {} (deployment: {})";
    if (service.isRequired()) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.warn(message, serviceId, deployment, failure);
      } else {
        LOGGER.warn(message + ": {}", serviceId, deployment, failureMessage);
      }
      return;
    }

    String optionalMessage = "Optional " + message;
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(optionalMessage, serviceId, deployment, failure);
    } else {
      LOGGER.info(optionalMessage + ": {}", serviceId, deployment, failureMessage);
    }
  }
}
