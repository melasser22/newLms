package com.ejada.gateway.admin;

import com.ejada.gateway.admin.model.AdminOverview;
import com.ejada.gateway.admin.model.AdminRouteView;
import com.ejada.gateway.admin.model.AdminServiceSnapshot;
import com.ejada.gateway.config.AdminAggregationProperties;
import com.ejada.gateway.config.GatewayRoutesProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
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

  public AdminAggregationService(WebClient.Builder webClientBuilder,
      AdminAggregationProperties adminProperties,
      GatewayRoutesProperties routesProperties) {
    this.webClientBuilder = webClientBuilder;
    this.adminProperties = adminProperties;
    this.routesProperties = routesProperties;
  }

  public Mono<AdminOverview> fetchOverview() {
    List<AdminAggregationProperties.Service> services = adminProperties.getAggregation().getServices();
    if (services.isEmpty()) {
      return Mono.just(AdminOverview.empty());
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
        .collectList()
        .map(AdminOverview::fromSnapshots);
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
}
