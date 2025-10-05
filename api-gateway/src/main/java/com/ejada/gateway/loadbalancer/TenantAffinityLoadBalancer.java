package com.ejada.gateway.loadbalancer;

import com.ejada.common.constants.HeaderNames;
import com.ejada.gateway.config.GatewayRoutesProperties;
import com.ejada.gateway.config.GatewayRoutesProperties.ServiceRoute;
import com.ejada.gateway.config.GatewayRoutesProperties.ServiceRoute.LoadBalancingStrategy;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultRequest;
import org.springframework.cloud.client.loadbalancer.DefaultResponse;
import org.springframework.cloud.client.loadbalancer.EmptyResponse;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.RequestData;
import org.springframework.cloud.client.loadbalancer.RequestDataContext;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.core.RoundRobinLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

/**
 * Custom load balancer that blends tenant affinity, consistent hashing, zone preference and
 * response-time aware weighting. The implementation falls back to round-robin when a route does not
 * opt-in via {@code gateway.routes[].lb-strategy = weighted-response-time}.
 */
public class TenantAffinityLoadBalancer implements ReactorServiceInstanceLoadBalancer {

  private static final Duration DEFAULT_STICKY_TTL = Duration.ofMinutes(15);

  private final String serviceId;
  private final ObjectProvider<ServiceInstanceListSupplier> supplierProvider;
  private final LoadBalancerHealthCheckAggregator aggregator;
  private final GatewayRoutesProperties routesProperties;
  private final RoundRobinLoadBalancer fallback;
  private final WebSocketStickTable stickTable;
  private final String localZone;

  public TenantAffinityLoadBalancer(String serviceId,
      ObjectProvider<ServiceInstanceListSupplier> supplierProvider,
      LoadBalancerHealthCheckAggregator aggregator,
      GatewayRoutesProperties routesProperties,
      WebSocketStickTable stickTable,
      String localZone) {
    this.serviceId = Objects.requireNonNull(serviceId, "serviceId");
    this.supplierProvider = Objects.requireNonNull(supplierProvider, "supplierProvider");
    this.aggregator = Objects.requireNonNull(aggregator, "aggregator");
    this.routesProperties = Objects.requireNonNull(routesProperties, "routesProperties");
    this.stickTable = (stickTable != null) ? stickTable : new WebSocketStickTable(DEFAULT_STICKY_TTL);
    this.localZone = localZone;
    this.fallback = new RoundRobinLoadBalancer(supplierProvider, serviceId);
  }

  @Override
  public Mono<Response<ServiceInstance>> choose(Request request) {
    ServiceInstanceListSupplier supplier = supplierProvider.getIfAvailable();
    if (supplier == null) {
      return Mono.just(new EmptyResponse());
    }
    return supplier.get(request)
        .next()
        .flatMap(instances -> selectInstance(request, instances));
  }

  private Mono<Response<ServiceInstance>> selectInstance(Request request, List<ServiceInstance> instances) {
    if (instances == null || instances.isEmpty()) {
      return Mono.just(new EmptyResponse());
    }
    if (!shouldUseWeightedStrategy(request)) {
      return fallback.choose(request);
    }

    List<WeightedInstance> candidates = instances.stream()
        .map(instance -> new WeightedInstance(instance, aggregator.update(instance)))
        .filter(candidate -> candidate.state().getAvailability() != LoadBalancerHealthCheckAggregator.Availability.DOWN)
        .filter(candidate -> candidate.state().getEffectiveWeight() > 0d)
        .sorted(Comparator.comparingDouble((WeightedInstance candidate) -> candidate.state().getEffectiveWeight()).reversed())
        .collect(Collectors.toCollection(ArrayList::new));

    if (candidates.isEmpty()) {
      return fallback.choose(request);
    }

    String tenantId = resolveTenantId(request);
    RequestData requestData = resolveRequestData(request);
    boolean websocket = requestData != null && isWebSocket(requestData.getHeaders());
    String connectionKey = websocket ? resolveStickinessKey(requestData, tenantId) : null;

    Optional<ServiceInstance> stickyMatch = Optional.empty();
    if (connectionKey != null) {
      stickyMatch = stickTable.lookup(connectionKey, serviceId,
          candidates.stream().map(WeightedInstance::instance).toList());
    }

    ServiceInstance chosen;
    if (stickyMatch.isPresent()) {
      chosen = stickyMatch.get();
    } else if (StringUtils.hasText(tenantId)) {
      chosen = selectByRendezvous(tenantId, candidates);
    } else {
      chosen = selectByWeight(candidates);
    }

    if (chosen == null) {
      return fallback.choose(request);
    }

    if (connectionKey != null) {
      stickTable.record(connectionKey, chosen);
    }

    return Mono.just(new DefaultResponse(chosen));
  }

  private boolean shouldUseWeightedStrategy(Request<?> request) {
    String routeId = resolveRouteId(request);
    if (!StringUtils.hasText(routeId)) {
      return false;
    }
    return routesProperties.findRouteById(routeId)
        .map(ServiceRoute::getLbStrategy)
        .map(strategy -> strategy == LoadBalancingStrategy.WEIGHTED_RESPONSE_TIME)
        .orElse(false);
  }

  private String resolveRouteId(Request<?> request) {
    RequestData requestData = resolveRequestData(request);
    if (requestData == null) {
      return null;
    }
    Object routeAttr = requestData.getAttributes().get(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
    if (routeAttr instanceof Route route) {
      return route.getId();
    }
    if (routeAttr instanceof String text && StringUtils.hasText(text)) {
      return text;
    }
    return null;
  }

  private RequestData resolveRequestData(Request<?> request) {
    if (request instanceof DefaultRequest<?> defaultRequest) {
      Object context = defaultRequest.getContext();
      if (context instanceof RequestDataContext dataContext) {
        return dataContext.getClientRequest();
      }
    }
    if (request instanceof RequestDataContext dataContext) {
      return dataContext.getClientRequest();
    }
    if (request != null && request.getContext() instanceof RequestDataContext dataContext) {
      return dataContext.getClientRequest();
    }
    return null;
  }

  private String resolveTenantId(Request<?> request) {
    RequestData requestData = resolveRequestData(request);
    if (requestData == null) {
      return null;
    }
    String tenant = requestData.getHeaders().getFirst(HeaderNames.X_TENANT_ID);
    if (StringUtils.hasText(tenant)) {
      return tenant.trim();
    }
    Object attributeTenant = requestData.getAttributes().get(HeaderNames.X_TENANT_ID);
    return attributeTenant instanceof String text && StringUtils.hasText(text) ? text.trim() : null;
  }

  private boolean isWebSocket(HttpHeaders headers) {
    if (headers == null) {
      return false;
    }
    String upgrade = headers.getFirst(HttpHeaders.UPGRADE);
    return StringUtils.hasText(upgrade) && "websocket".equalsIgnoreCase(upgrade.trim());
  }

  private String resolveStickinessKey(RequestData requestData, String tenantId) {
    HttpHeaders headers = requestData.getHeaders();
    String websocketKey = headers.getFirst("Sec-WebSocket-Key");
    String connectionId = headers.getFirst("X-Connection-Id");
    String base = StringUtils.hasText(websocketKey) ? websocketKey : connectionId;
    if (!StringUtils.hasText(base)) {
      URI uri = requestData.getUrl();
      base = (uri != null) ? uri.toString() : "";
    }
    String tenant = StringUtils.hasText(tenantId) ? tenantId : "anonymous";
    return tenant + ':' + base;
  }

  private ServiceInstance selectByRendezvous(String key, List<WeightedInstance> candidates) {
    List<WeightedInstance> scoped = preferZone(candidates);
    double bestScore = Double.NEGATIVE_INFINITY;
    WeightedInstance best = null;
    for (WeightedInstance candidate : scoped) {
      double weight = candidate.state().getEffectiveWeight();
      if (weight <= 0d) {
        continue;
      }
      double hash = unitHash(key, candidate.instanceKey());
      double score = weight / -Math.log(hash == 0d ? Double.MIN_VALUE : hash);
      if (score > bestScore) {
        bestScore = score;
        best = candidate;
      }
    }
    if (best != null) {
      return best.instance();
    }
    return selectByWeight(candidates);
  }

  private ServiceInstance selectByWeight(List<WeightedInstance> candidates) {
    List<WeightedInstance> scoped = preferZone(candidates);
    double total = scoped.stream().mapToDouble(candidate -> Math.max(0d, candidate.state().getEffectiveWeight())).sum();
    if (total <= 0d) {
      return candidates.isEmpty() ? null : candidates.get(0).instance();
    }
    double threshold = ThreadLocalRandom.current().nextDouble(total);
    double running = 0d;
    for (WeightedInstance candidate : scoped) {
      running += Math.max(0d, candidate.state().getEffectiveWeight());
      if (running >= threshold) {
        return candidate.instance();
      }
    }
    return scoped.get(scoped.size() - 1).instance();
  }

  private List<WeightedInstance> preferZone(List<WeightedInstance> candidates) {
    if (!StringUtils.hasText(localZone)) {
      return candidates;
    }
    List<WeightedInstance> sameZone = candidates.stream()
        .filter(candidate -> localZone.equalsIgnoreCase(candidate.state().getZone()))
        .collect(Collectors.toList());
    return sameZone.isEmpty() ? candidates : sameZone;
  }

  private double unitHash(String tenantKey, String instanceId) {
    byte[] data = (tenantKey + '|' + instanceId).getBytes(StandardCharsets.UTF_8);
    long hash = 1469598103934665603L; // FNV-1a 64-bit offset basis
    for (byte b : data) {
      hash ^= (b & 0xff);
      hash *= 1099511628211L;
    }
    long bits = (hash >>> 11) | 0x3ff0000000000000L;
    double value = Double.longBitsToDouble(bits) - 1.0d;
    if (value <= 0d || value >= 1d) {
      return 0.5d;
    }
    return value;
  }

  private record WeightedInstance(ServiceInstance instance, LoadBalancerHealthCheckAggregator.InstanceState state) {

    String instanceKey() {
      return state.getInstanceId();
    }
  }
}
