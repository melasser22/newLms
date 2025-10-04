package com.ejada.gateway.admin;

import com.ejada.gateway.admin.model.AdminOverview;
import com.ejada.gateway.admin.model.AdminRouteView;
import com.ejada.gateway.admin.model.AdminServiceSnapshot;
import com.ejada.gateway.admin.model.DetailedHealthStatus;
import com.ejada.gateway.admin.model.DetailedHealthStatus.CircuitBreakerHealth;
import com.ejada.gateway.admin.model.DetailedHealthStatus.RedisHealthStatus;
import com.ejada.gateway.config.AdminAggregationProperties;
import com.ejada.gateway.config.GatewayRoutesProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.annotation.Timed;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
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

  public AdminAggregationService(WebClient.Builder webClientBuilder,
      AdminAggregationProperties adminProperties,
      GatewayRoutesProperties routesProperties,
      ObjectProvider<ReactiveStringRedisTemplate> redisTemplateProvider,
      ObjectProvider<CircuitBreakerRegistry> circuitBreakerRegistryProvider) {
    this.webClientBuilder = webClientBuilder;
    this.adminProperties = adminProperties;
    this.routesProperties = routesProperties;
    this.redisTemplate = redisTemplateProvider.getIfAvailable();
    this.circuitBreakerRegistry = circuitBreakerRegistryProvider.getIfAvailable();
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

  @Timed(value = "gateway.admin.downstream.snapshots", description = "Downstream service snapshot aggregation")
  public Mono<List<AdminServiceSnapshot>> collectDownstreamSnapshots() {
    List<AdminAggregationProperties.Service> services = adminProperties.getAggregation().getServices();
    if (services.isEmpty()) {
      return Mono.just(List.of());
    }

    IntStream.range(0, services.size()).forEach(index -> {
      AdminAggregationProperties.Service service = services.get(index);
      String key = service.getId() != null ? service.getId() : String.valueOf(index);
      service.validate(key);
    });

    Duration timeout = adminProperties.getAggregation().getTimeout();

    return Flux.fromIterable(services)
        .flatMap(service -> fetchSnapshot(service, timeout))
        .sort(Comparator.comparing(AdminServiceSnapshot::serviceId))
        .collectList();
  }

  @Timed(value = "gateway.admin.health.detailed", description = "Detailed gateway health aggregation")
  public Mono<DetailedHealthStatus> fetchDetailedHealth() {
    Mono<RedisHealthStatus> redisHealth = checkRedisHealth();
    Mono<List<AdminServiceSnapshot>> downstream = collectDownstreamSnapshots();
    Mono<List<CircuitBreakerHealth>> circuitBreakers = Mono.fromSupplier(this::collectCircuitBreakerStates);

    return Mono.zip(redisHealth, downstream, circuitBreakers)
        .map(tuple -> new DetailedHealthStatus(tuple.getT1(), tuple.getT2(), tuple.getT3()))
        .defaultIfEmpty(DetailedHealthStatus.empty());
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

    return client.get()
        .uri(service.getHealthPath())
        .headers(httpHeaders -> service.getHeaders().forEach(httpHeaders::add))
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
          LOGGER.warn("Admin aggregation failed for {}", service.getId(), ex);
          return Mono.just(AdminServiceSnapshot.failure(
              service.getId(),
              service.getDeployment(),
              service.isRequired(),
              ex,
              Instant.now()));
        });
  }
}
