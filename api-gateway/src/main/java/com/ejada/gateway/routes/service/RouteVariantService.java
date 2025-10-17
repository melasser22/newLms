package com.ejada.gateway.routes.service;

import com.ejada.gateway.config.GatewayRoutesProperties;
import com.ejada.gateway.routes.model.RouteDefinition;
import com.ejada.gateway.routes.model.RouteMetadata;
import com.ejada.gateway.routes.model.RouteVariantMetricsView;
import com.ejada.gateway.routes.repository.RouteDefinitionRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Tracks runtime metrics for dynamically registered route variants (canary, A/B tests) and
 * orchestrates automatic rollback when canary error rates exceed the allowed threshold.
 */
@Component
public class RouteVariantService {

  private static final Logger LOGGER = LoggerFactory.getLogger(RouteVariantService.class);

  private static final Duration CANARY_ERROR_WINDOW = Duration.ofMinutes(2);
  private static final double CANARY_ERROR_THRESHOLD = 0.05d;
  private static final int MIN_CANARY_SAMPLES = 50;

  private final RouteDefinitionRepository repository;
  private final Clock clock;

  private final ConcurrentMap<String, VariantRegistration> variants = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, VariantMetrics> metrics = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, RollingWindow> canaryWindows = new ConcurrentHashMap<>();
  private final Set<UUID> rollbackInFlight = ConcurrentHashMap.newKeySet();

  @Autowired
  public RouteVariantService(RouteDefinitionRepository repository) {
    this(repository, Clock.systemUTC());
  }

  RouteVariantService(RouteDefinitionRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
  }

  /**
   * Rebuilds the variant registry based on the currently active routes.
   */
  public void rebuild(List<RouteDefinition> definitions,
      Map<UUID, List<GatewayRoutesProperties.ServiceRoute>> runtimeRoutes) {
    if (definitions == null) {
      return;
    }
    Set<String> activeVariants = new HashSet<>();
    Set<UUID> activeRouteIds = new HashSet<>();
    for (RouteDefinition definition : definitions) {
      if (definition == null || definition.id() == null) {
        continue;
      }
      UUID routeId = definition.id();
      activeRouteIds.add(routeId);
      List<GatewayRoutesProperties.ServiceRoute> serviceRoutes = runtimeRoutes.getOrDefault(routeId,
          List.of());
      Map<String, RouteMetadata.TrafficSplit> splits = indexSplits(definition.metadata());
      if (serviceRoutes.isEmpty()) {
        // still expose metrics for single variant routes
        variants.put(definition.id().toString(), new VariantRegistration(routeId, "primary", false,
            100));
        activeVariants.add(definition.id().toString());
        continue;
      }
      for (GatewayRoutesProperties.ServiceRoute serviceRoute : serviceRoutes) {
        if (serviceRoute == null || !StringUtils.hasText(serviceRoute.getId())) {
          continue;
        }
        String gatewayRouteId = serviceRoute.getId();
        String variantId = resolveVariantId(serviceRoute, splits);
        RouteMetadata.TrafficSplit split = splits.get(variantId);
        boolean canary = split != null && "canary".equalsIgnoreCase(split.getVariantId());
        int percentage = (split != null) ? split.getPercentage() : 100;
        variants.put(gatewayRouteId, new VariantRegistration(routeId, variantId, canary, percentage));
        activeVariants.add(gatewayRouteId);
      }
    }
    variants.keySet().retainAll(activeVariants);
    metrics.keySet().retainAll(activeVariants);
    canaryWindows.keySet().retainAll(activeVariants);
    rollbackInFlight.retainAll(activeRouteIds);
  }

  private Map<String, RouteMetadata.TrafficSplit> indexSplits(RouteMetadata metadata) {
    if (metadata == null || CollectionUtils.isEmpty(metadata.getTrafficSplits())) {
      return Collections.emptyMap();
    }
    Map<String, RouteMetadata.TrafficSplit> indexed = new HashMap<>();
    for (RouteMetadata.TrafficSplit split : metadata.getTrafficSplits()) {
      if (split == null) {
        continue;
      }
      String key = RouteDefinitionConverter.normaliseVariant(split.getVariantId());
      indexed.put(key, split);
    }
    return indexed;
  }

  private String resolveVariantId(GatewayRoutesProperties.ServiceRoute serviceRoute,
      Map<String, RouteMetadata.TrafficSplit> splits) {
    String variantId = serviceRoute.getVariantId();
    if (!StringUtils.hasText(variantId)) {
      String lookup = RouteDefinitionConverter.normaliseVariant(serviceRoute.getId());
      RouteMetadata.TrafficSplit split = splits.get(lookup);
      if (split != null && StringUtils.hasText(split.getVariantId())) {
        variantId = split.getVariantId();
      }
    }
    if (!StringUtils.hasText(variantId)) {
      return "primary";
    }
    return variantId.trim();
  }

  /** Records an HTTP outcome for the supplied route variant. */
  public void recordResult(String gatewayRouteId, int statusCode) {
    if (!StringUtils.hasText(gatewayRouteId)) {
      return;
    }
    VariantRegistration registration = variants.get(gatewayRouteId);
    if (registration == null) {
      return;
    }
    VariantMetrics metric = metrics.computeIfAbsent(gatewayRouteId, key -> new VariantMetrics());
    metric.record(statusCode);

    if (registration.canary()) {
      RollingWindow window = canaryWindows.computeIfAbsent(gatewayRouteId,
          key -> new RollingWindow(CANARY_ERROR_WINDOW, clock));
      boolean success = statusCode < 500;
      window.record(success);
      double errorRate = window.errorRate();
      if (!success && window.totalSamples() >= MIN_CANARY_SAMPLES
          && errorRate > CANARY_ERROR_THRESHOLD) {
        triggerRollback(registration, errorRate);
      }
    }
  }

  private void triggerRollback(VariantRegistration registration, double errorRate) {
    if (!rollbackInFlight.add(registration.routeId())) {
      return;
    }
    LOGGER.warn("Canary route {} for {} exceeded error threshold ({}). Triggering rollback.",
        registration.variantId(), registration.routeId(), errorRate);
    repository.findById(registration.routeId())
        .flatMap(route -> {
          RouteMetadata metadata = route.metadata() != null ? route.metadata().copy() : new RouteMetadata();
          boolean removed = metadata.getTrafficSplits().removeIf(split ->
              split != null && "canary".equalsIgnoreCase(split.getVariantId()));
          if (!removed) {
            return Mono.empty();
          }
          RouteDefinition updated = route.withMetadata(metadata);
          return repository.update(updated, "system-canary-rollback");
        })
        .doOnSuccess(route -> LOGGER.info("Canary rollback complete for route {}", registration.routeId()))
        .doOnError(ex -> LOGGER.error("Failed to rollback canary route {}", registration.routeId(), ex))
        .doFinally(signal -> rollbackInFlight.remove(registration.routeId()))
        .subscribeOn(Schedulers.boundedElastic())
        .subscribe();
  }

  public List<RouteVariantMetricsView> snapshotMetrics() {
    List<RouteVariantMetricsView> snapshot = new ArrayList<>();
    variants.forEach((gatewayRouteId, registration) -> {
      VariantMetrics metric = metrics.getOrDefault(gatewayRouteId, VariantMetrics.EMPTY);
      snapshot.add(toView(gatewayRouteId, registration, metric));
    });
    snapshot.sort((a, b) -> {
      int compare = a.routeId().compareTo(b.routeId());
      if (compare != 0) {
        return compare;
      }
      return a.variantId().compareToIgnoreCase(b.variantId());
    });
    return snapshot;
  }

  public List<RouteVariantMetricsView> metricsForRoute(UUID routeId) {
    if (routeId == null) {
      return List.of();
    }
    List<RouteVariantMetricsView> snapshot = new ArrayList<>();
    variants.forEach((gatewayRouteId, registration) -> {
      if (!routeId.equals(registration.routeId())) {
        return;
      }
      VariantMetrics metric = metrics.getOrDefault(gatewayRouteId, VariantMetrics.EMPTY);
      snapshot.add(toView(gatewayRouteId, registration, metric));
    });
    snapshot.sort((a, b) -> a.variantId().compareToIgnoreCase(b.variantId()));
    return snapshot;
  }

  private RouteVariantMetricsView toView(String gatewayRouteId, VariantRegistration registration,
      VariantMetrics metric) {
    double errorRate = metric.errorRate();
    double conversionRate = metric.conversionRate();
    return new RouteVariantMetricsView(
        registration.routeId(),
        registration.variantId(),
        gatewayRouteId,
        registration.canary(),
        metric.requests(),
        metric.successes(),
        metric.errors(),
        metric.conversions(),
        errorRate,
        conversionRate);
  }

  private record VariantRegistration(UUID routeId, String variantId, boolean canary, int percentage) {
  }

  private static final class VariantMetrics {

    static final VariantMetrics EMPTY = new VariantMetrics(true);

    private final AtomicLong requests = new AtomicLong();
    private final AtomicLong successes = new AtomicLong();
    private final AtomicLong errors = new AtomicLong();
    private final AtomicLong conversions = new AtomicLong();
    private final boolean frozen;

    private VariantMetrics() {
      this(false);
    }

    private VariantMetrics(boolean frozen) {
      this.frozen = frozen;
    }

    void record(int statusCode) {
      if (frozen) {
        return;
      }
      requests.incrementAndGet();
      if (statusCode >= 200 && statusCode < 500) {
        successes.incrementAndGet();
      }
      if (statusCode >= 500) {
        errors.incrementAndGet();
      }
      if (statusCode >= 200 && statusCode < 300) {
        conversions.incrementAndGet();
      }
    }

    long requests() {
      return requests.get();
    }

    long successes() {
      return successes.get();
    }

    long errors() {
      return errors.get();
    }

    long conversions() {
      return conversions.get();
    }

    double errorRate() {
      long total = requests.get();
      if (total == 0) {
        return 0d;
      }
      return (double) errors.get() / total;
    }

    double conversionRate() {
      long total = requests.get();
      if (total == 0) {
        return 0d;
      }
      return (double) conversions.get() / total;
    }
  }

  private static final class RollingWindow {

    private final Duration window;
    private final Clock clock;
    private final ArrayDeque<Result> samples = new ArrayDeque<>();
    private int successes;
    private int failures;

    private RollingWindow(Duration window, Clock clock) {
      this.window = Objects.requireNonNull(window, "window");
      this.clock = Objects.requireNonNull(clock, "clock");
    }

    synchronized void record(boolean success) {
      Instant now = Instant.now(clock);
      samples.addLast(new Result(now, success));
      if (success) {
        successes++;
      } else {
        failures++;
      }
      purge(now);
    }

    synchronized double errorRate() {
      purge(Instant.now(clock));
      int total = successes + failures;
      if (total == 0) {
        return 0d;
      }
      return (double) failures / total;
    }

    synchronized int totalSamples() {
      purge(Instant.now(clock));
      return successes + failures;
    }

    private void purge(Instant now) {
      Instant threshold = now.minus(window);
      while (!samples.isEmpty() && samples.peekFirst().timestamp().isBefore(threshold)) {
        Result evicted = samples.removeFirst();
        if (evicted.success()) {
          successes--;
        } else {
          failures--;
        }
      }
    }

    private record Result(Instant timestamp, boolean success) {
    }
  }
}
