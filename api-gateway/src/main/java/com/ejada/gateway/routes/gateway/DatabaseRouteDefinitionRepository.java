package com.ejada.gateway.routes.gateway;

import com.ejada.gateway.routes.model.RouteComponent;
import com.ejada.gateway.routes.model.RouteDefinition;
import com.ejada.gateway.routes.model.RouteMetadata;
import com.ejada.gateway.routes.model.RouteMetadata.BlueGreenDeployment;
import com.ejada.gateway.routes.model.RouteMetadata.TrafficSplit;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * {@link org.springframework.cloud.gateway.route.RouteDefinitionRepository} implementation that
 * reads active route definitions from the database and exposes them to Spring Cloud Gateway.
 *
 * <p>The routes are cached for a short duration to avoid hammering the database on every refresh.
 * A {@link RefreshRoutesEvent} will invalidate the cache and trigger a reload on the next
 * subscription.</p>
 */
@Component
public class DatabaseRouteDefinitionRepository
    implements org.springframework.cloud.gateway.route.RouteDefinitionRepository {

  private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseRouteDefinitionRepository.class);
  private static final Duration CACHE_TTL = Duration.ofMinutes(5);

  private final com.ejada.gateway.routes.repository.RouteDefinitionRepository repository;
  private final AtomicReference<Flux<org.springframework.cloud.gateway.route.RouteDefinition>> cachedRoutes =
      new AtomicReference<>();

  public DatabaseRouteDefinitionRepository(
      com.ejada.gateway.routes.repository.RouteDefinitionRepository repository) {
    this.repository = repository;
  }

  @Override
  public Flux<org.springframework.cloud.gateway.route.RouteDefinition> getRouteDefinitions() {
    Flux<org.springframework.cloud.gateway.route.RouteDefinition> cached = cachedRoutes.get();
    if (cached != null) {
      return cached;
    }

    Flux<org.springframework.cloud.gateway.route.RouteDefinition> loader = repository.findActiveRoutes()
        .doOnSubscribe(subscription -> LOGGER.debug("Loading active gateway routes from database"))
        .flatMap(route -> Mono.fromCallable(() -> toGatewayRoute(route))
            .onErrorResume(ex -> {
              LOGGER.warn("Skipping route {} due to conversion error", route.id(), ex);
              return Mono.empty();
            }))
        .onErrorResume(ex -> {
          LOGGER.warn("Failed to fetch route definitions from database", ex);
          return Flux.empty();
        })
        .cache(CACHE_TTL);

    if (cachedRoutes.compareAndSet(null, loader)) {
      return loader;
    }
    return cachedRoutes.get();
  }

  @Override
  public Mono<Void> save(Mono<org.springframework.cloud.gateway.route.RouteDefinition> route) {
    return Mono.error(new UnsupportedOperationException(
        "Mutations are managed via the /admin/routes API and cannot be performed directly."));
  }

  @Override
  public Mono<Void> delete(Mono<String> routeId) {
    return Mono.error(new UnsupportedOperationException(
        "Mutations are managed via the /admin/routes API and cannot be performed directly."));
  }

  @EventListener(RefreshRoutesEvent.class)
  public void handleRefreshEvent(RefreshRoutesEvent event) {
    LOGGER.debug("Clearing cached database routes due to {}", event.getClass().getSimpleName());
    cachedRoutes.set(null);
  }

  private org.springframework.cloud.gateway.route.RouteDefinition toGatewayRoute(RouteDefinition route) {
    RouteDefinition resolved = route.ensureServiceUri();
    org.springframework.cloud.gateway.route.RouteDefinition gateway =
        new org.springframework.cloud.gateway.route.RouteDefinition();
    gateway.setId(resolved.id().toString());
    gateway.setUri(resolved.serviceUri());
    gateway.setPredicates(resolved.predicates().stream()
        .map(this::toPredicate)
        .toList());
    gateway.setFilters(resolved.filters().stream()
        .map(this::toFilter)
        .toList());
    gateway.setMetadata(buildMetadata(resolved));
    return gateway;
  }

  private PredicateDefinition toPredicate(RouteComponent component) {
    PredicateDefinition predicate = new PredicateDefinition();
    predicate.setName(component.name());
    predicate.setArgs(new LinkedHashMap<>(component.args()));
    return predicate;
  }

  private FilterDefinition toFilter(RouteComponent component) {
    FilterDefinition filter = new FilterDefinition();
    filter.setName(component.name());
    filter.setArgs(new LinkedHashMap<>(component.args()));
    return filter;
  }

  private Map<String, Object> buildMetadata(RouteDefinition route) {
    Map<String, Object> metadata = new LinkedHashMap<>();
    metadata.put("pathPattern", route.pathPattern());
    metadata.put("enabled", route.enabled());
    metadata.put("version", route.version());
    metadata.put("createdAt", route.createdAt());
    metadata.put("updatedAt", route.updatedAt());

    RouteMetadata details = route.metadata();
    if (details != null) {
      if (!CollectionUtils.isEmpty(details.getRequestHeaders())) {
        metadata.put("requestHeaders", new LinkedHashMap<>(details.getRequestHeaders()));
      }
      if (!CollectionUtils.isEmpty(details.getMethods())) {
        metadata.put("methods", new ArrayList<>(details.getMethods()));
      }
      if (details.getStripPrefix() != null) {
        metadata.put("stripPrefix", details.getStripPrefix());
      }
      if (StringUtils.hasText(details.getPrefixPath())) {
        metadata.put("prefixPath", details.getPrefixPath());
      }
      if (!CollectionUtils.isEmpty(details.getAttributes())) {
        details.getAttributes().forEach(metadata::putIfAbsent);
      }
      Map<String, Object> blueGreen = toBlueGreen(details.getBlueGreen());
      if (!blueGreen.isEmpty()) {
        metadata.put("blueGreen", blueGreen);
      }
      List<Map<String, Object>> trafficSplits = toTrafficSplits(details.getTrafficSplits());
      if (!trafficSplits.isEmpty()) {
        metadata.put("trafficSplits", trafficSplits);
      }
    }
    return metadata;
  }

  private Map<String, Object> toBlueGreen(BlueGreenDeployment deployment) {
    Map<String, Object> blueGreen = new LinkedHashMap<>();
    if (deployment == null) {
      return blueGreen;
    }
    deployment.getActiveSlot().ifPresent(slot -> blueGreen.put("activeSlot", slot));
    if (deployment.getBlueUri() != null) {
      blueGreen.put("blueUri", deployment.getBlueUri());
    }
    if (deployment.getGreenUri() != null) {
      blueGreen.put("greenUri", deployment.getGreenUri());
    }
    return blueGreen;
  }

  private List<Map<String, Object>> toTrafficSplits(List<TrafficSplit> splits) {
    if (CollectionUtils.isEmpty(splits)) {
      return List.of();
    }
    List<Map<String, Object>> mapped = new ArrayList<>();
    for (TrafficSplit split : splits) {
      if (split == null) {
        continue;
      }
      Map<String, Object> entry = new LinkedHashMap<>();
      if (StringUtils.hasText(split.getVariantId())) {
        entry.put("variantId", split.getVariantId());
      }
      entry.put("percentage", split.getPercentage());
      if (split.getServiceUri() != null) {
        entry.put("serviceUri", split.getServiceUri());
      }
      mapped.add(entry);
    }
    return mapped;
  }
}
