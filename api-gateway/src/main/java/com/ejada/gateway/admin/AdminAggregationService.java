package com.ejada.gateway.admin;

import com.ejada.gateway.admin.model.AdminOverview;
import com.ejada.gateway.admin.model.AdminRouteView;
import com.ejada.gateway.admin.model.AdminServiceSnapshot;
import com.ejada.gateway.admin.model.DetailedHealthResponse;
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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.connection.ReactiveRedisConnection;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
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
      @Nullable ReactiveStringRedisTemplate redisTemplate,
      @Nullable CircuitBreakerRegistry circuitBreakerRegistry) {
    this.webClientBuilder = webClientBuilder;
    this.adminProperties = adminProperties;
    this.routesProperties = routesProperties;
    this.redisTemplate = redisTemplate;
    this.circuitBreakerRegistry = circuitBreakerRegistry;
  }

  @Timed(value = "gateway.admin.fetchOverview", description = "Aggregated admin overview retrieval")
  public Mono<AdminOverview> fetchOverview() {
    List<AdminAggregationProperties.Service> services = configuredServices();
    if (services.isEmpty()) {
      return Mono.just(AdminOverview.empty());
    }

    Duration timeout = adminProperties.getAggregation().getTimeout();

    return Flux.fromIterable(services)
        .flatMap(service -> fetchSnapshot(service, timeout))
        .sort(Comparator.comparing(AdminServiceSnapshot::serviceId))
        .collectList()
        .map(AdminOverview::fromSnapshots);
  }

  @Timed(value = "gateway.admin.detailedHealth", description = "Detailed dependency health aggregation")
  public Mono<DetailedHealthResponse> fetchDetailedHealth() {
    List<AdminAggregationProperties.Service> services = configuredServices();
    Duration timeout = adminProperties.getAggregation().getTimeout();

    Mono<DetailedHealthResponse.ComponentHealth> redisHealth = checkRedisConnectivity();
    Mono<List<DetailedHealthResponse.ComponentHealth>> downstreamHealth = Flux.fromIterable(services)
        .flatMap(service -> fetchSnapshot(service, timeout)
            .map(DetailedHealthResponse.ComponentHealth::fromSnapshot)
            .defaultIfEmpty(DetailedHealthResponse.ComponentHealth.unreachable(service)))
        .collectList();
    Mono<List<DetailedHealthResponse.CircuitBreakerHealth>> circuitBreakers = collectCircuitBreakerStates();

    return Mono.zip(redisHealth, downstreamHealth, circuitBreakers)
        .map(tuple -> DetailedHealthResponse.compose(tuple.getT1(), tuple.getT2(), tuple.getT3()));
  }

  public List<AdminRouteView> describeRoutes() {
    return routesProperties.getRoutes().values().stream()
        .sorted(Comparator.comparing(route -> {
          String id = route.getId();
          return id == null ? "" : id;
        }))
        .map(AdminRouteView::fromRoute)
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

  private Mono<DetailedHealthResponse.ComponentHealth> checkRedisConnectivity() {
    if (redisTemplate == null || redisTemplate.getConnectionFactory() == null) {
      return Mono.just(DetailedHealthResponse.ComponentHealth.down("redis", true, -1, "Connection factory unavailable"));
    }
    return Mono.usingWhen(
            Mono.fromCallable(() -> redisTemplate.getConnectionFactory().getReactiveConnection()),
            connection -> probeRedis(connection).onErrorResume(ex -> Mono.just(
                DetailedHealthResponse.ComponentHealth.down("redis", true, -1, ex.getMessage()))),
            ReactiveRedisConnection::close)
        .onErrorResume(ex -> Mono.just(DetailedHealthResponse.ComponentHealth.down("redis", true, -1, ex.getMessage())));
  }

  private Mono<DetailedHealthResponse.ComponentHealth> probeRedis(ReactiveRedisConnection connection) {
    long start = System.nanoTime();
    return connection.serverCommands()
        .ping()
        .map(response -> DetailedHealthResponse.ComponentHealth.up("redis", true, millisSince(start), response))
        .timeout(Duration.ofSeconds(2))
        .onErrorResume(ex -> Mono.just(DetailedHealthResponse.ComponentHealth.down("redis", true,
            millisSince(start), ex.getMessage())));
  }

  private Mono<List<DetailedHealthResponse.CircuitBreakerHealth>> collectCircuitBreakerStates() {
    if (circuitBreakerRegistry == null) {
      return Mono.just(List.of());
    }
    return Mono.fromSupplier(() -> circuitBreakerRegistry.getAllCircuitBreakers().stream()
        .map(breaker -> new DetailedHealthResponse.CircuitBreakerHealth(breaker.getName(), breaker.getState().name()))
        .sorted(Comparator.comparing(DetailedHealthResponse.CircuitBreakerHealth::serviceName))
        .toList());
  }

  private List<AdminAggregationProperties.Service> configuredServices() {
    List<AdminAggregationProperties.Service> services = adminProperties.getAggregation().getServices();
    IntStream.range(0, services.size()).forEach(index -> {
      AdminAggregationProperties.Service service = services.get(index);
      String key = service.getId() != null ? service.getId() : String.valueOf(index);
      service.validate(key);
    });
    return services;
  }

  private static long millisSince(long start) {
    return Duration.ofNanos(System.nanoTime() - start).toMillis();
  }
}
