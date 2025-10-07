package com.ejada.gateway.loadbalancer;

import com.ejada.gateway.config.GatewayRoutesProperties;
import com.ejada.gateway.config.GatewayRoutesProperties.ServiceRoute;
import com.ejada.gateway.config.GatewayRoutesProperties.ServiceRoute.LoadBalancingStrategy;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultResponse;
import org.springframework.cloud.client.loadbalancer.EmptyResponse;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.core.RoundRobinLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

/**
 * Orchestrates the load-balancing pipeline by applying a sequence of {@link InstanceFilter filters}
 * followed by {@link InstanceSelector selectors}. If no selector can produce a server, the
 * configured fallback (standard round-robin) is used.
 */
public class CompositeLoadBalancer implements ReactorServiceInstanceLoadBalancer {

  private final String serviceId;
  private final ObjectProvider<ServiceInstanceListSupplier> supplierProvider;
  private final LoadBalancerHealthCheckAggregator aggregator;
  private final GatewayRoutesProperties routesProperties;
  private final List<InstanceFilter> filters;
  private final List<InstanceSelector> selectors;
  private final ReactorServiceInstanceLoadBalancer fallback;

  public CompositeLoadBalancer(String serviceId,
      ObjectProvider<ServiceInstanceListSupplier> supplierProvider,
      LoadBalancerHealthCheckAggregator aggregator,
      GatewayRoutesProperties routesProperties,
      List<InstanceFilter> filters,
      List<InstanceSelector> selectors) {
    this(serviceId, supplierProvider, aggregator, routesProperties, filters, selectors,
        new RoundRobinLoadBalancer(supplierProvider, serviceId));
  }

  public CompositeLoadBalancer(String serviceId,
      ObjectProvider<ServiceInstanceListSupplier> supplierProvider,
      LoadBalancerHealthCheckAggregator aggregator,
      GatewayRoutesProperties routesProperties,
      List<InstanceFilter> filters,
      List<InstanceSelector> selectors,
      ReactorServiceInstanceLoadBalancer fallback) {
    this.serviceId = Objects.requireNonNull(serviceId, "serviceId");
    this.supplierProvider = Objects.requireNonNull(supplierProvider, "supplierProvider");
    this.aggregator = Objects.requireNonNull(aggregator, "aggregator");
    this.routesProperties = Objects.requireNonNull(routesProperties, "routesProperties");
    this.filters = List.copyOf(filters);
    this.selectors = List.copyOf(selectors);
    this.fallback = Objects.requireNonNull(fallback, "fallback");
  }

  @Override
  @SuppressWarnings("rawtypes")
  public Mono<Response<ServiceInstance>> choose(Request request) {
    ServiceInstanceListSupplier supplier = supplierProvider.getIfAvailable();
    if (supplier == null) {
      return Mono.just(new EmptyResponse());
    }

    if (!shouldUseCustomStrategy(request)) {
      return fallback.choose(request);
    }

    return supplier.get(request)
        .next()
        .flatMap(instances -> selectInstance(request, instances));
  }

  private boolean shouldUseCustomStrategy(Request<?> request) {
    String routeId = LoadBalancerRequestAdapter.resolveRouteId(request);
    if (!StringUtils.hasText(routeId)) {
      return false;
    }
    return routesProperties.findRouteById(routeId)
        .map(ServiceRoute::getLbStrategy)
        .map(strategy -> strategy == LoadBalancingStrategy.WEIGHTED_RESPONSE_TIME)
        .orElse(false);
  }

  private Mono<Response<ServiceInstance>> selectInstance(Request<?> request, List<ServiceInstance> instances) {
    if (instances == null || instances.isEmpty()) {
      return Mono.just(new EmptyResponse());
    }

    List<InstanceCandidate> candidates = new ArrayList<>(instances.size());
    for (ServiceInstance instance : instances) {
      candidates.add(new InstanceCandidate(instance, aggregator.update(instance)));
    }

    List<InstanceCandidate> filtered = candidates;
    for (InstanceFilter filter : filters) {
      filtered = filter.filter(serviceId, request, filtered);
      if (filtered.isEmpty()) {
        return fallback.choose(request);
      }
    }

    for (InstanceSelector selector : selectors) {
      Optional<ServiceInstance> selected = selector.select(serviceId, request, filtered);
      if (selected.isPresent()) {
        return Mono.just(new DefaultResponse(selected.get()));
      }
    }

    return fallback.choose(request);
  }
}
