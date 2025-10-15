package com.ejada.gateway.loadbalancer;

import com.ejada.gateway.config.GatewayRoutesProperties;
import com.ejada.gateway.config.GatewayRoutesProperties.ServiceRoute;
import com.ejada.gateway.config.GatewayRoutesProperties.ServiceRoute.LoadBalancingStrategy;
import com.ejada.gateway.loadbalancer.TenantMigrationService.ResponseWrapper;
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
  private final TenantMigrationService migrationService;
  private final TenantContext tenantContext;

  public CompositeLoadBalancer(String serviceId,
      ObjectProvider<ServiceInstanceListSupplier> supplierProvider,
      LoadBalancerHealthCheckAggregator aggregator,
      GatewayRoutesProperties routesProperties,
      TenantMigrationService migrationService,
      TenantContext tenantContext,
      List<InstanceFilter> filters,
      List<InstanceSelector> selectors) {
    this(serviceId, supplierProvider, aggregator, routesProperties, migrationService, tenantContext, filters, selectors,
        new RoundRobinLoadBalancer(supplierProvider, serviceId));
  }

  public CompositeLoadBalancer(String serviceId,
      ObjectProvider<ServiceInstanceListSupplier> supplierProvider,
      LoadBalancerHealthCheckAggregator aggregator,
      GatewayRoutesProperties routesProperties,
      TenantMigrationService migrationService,
      TenantContext tenantContext,
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
    this.migrationService = migrationService;
    this.tenantContext = tenantContext;
  }

  @Override
  @SuppressWarnings("rawtypes")
  public Mono<Response<ServiceInstance>> choose(Request request) {
    if (!shouldUseCustomStrategy(request)) {
      return fallback.choose(request);
    }

    ServiceInstanceListSupplier supplier = supplierProvider.getIfAvailable();
    if (supplier == null) {
      return Mono.just(new EmptyResponse());
    }

    Mono<Response<ServiceInstance>> baseSelection = chooseFromSupplier(serviceId, supplier, request)
        .switchIfEmpty(fallback.choose(request));

    if (migrationService == null) {
      return baseSelection;
    }

    String tenantId = LoadBalancerRequestAdapter.resolveTenantId(request, tenantContext);
    Optional<String> targetService = migrationService.resolveTarget(serviceId, tenantId);
    if (targetService.isEmpty() || serviceId.equals(targetService.get())) {
      return baseSelection;
    }

    return migrationService.chooseFromTarget(targetService.get(), request, filters, selectors)
        .map(wrapper -> (Response<ServiceInstance>) new DefaultResponse(wrapper.instance()))
        .switchIfEmpty(baseSelection);
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

  private Mono<Response<ServiceInstance>> chooseFromSupplier(String targetServiceId,
                                                            ServiceInstanceListSupplier supplier,
                                                            Request<?> request) {
    return supplier.get(request)
        .next()
        .flatMap(instances -> selectInstance(targetServiceId, request, instances))
        .switchIfEmpty(Mono.empty());
  }

  private Mono<Response<ServiceInstance>> selectInstance(String targetServiceId,
                                                         Request<?> request,
                                                         List<ServiceInstance> instances) {
    if (instances == null || instances.isEmpty()) {
      return Mono.empty();
    }

    List<InstanceCandidate> candidates = new ArrayList<>(instances.size());
    for (ServiceInstance instance : instances) {
      candidates.add(new InstanceCandidate(instance, aggregator.update(instance)));
    }

    List<InstanceCandidate> filtered = candidates;
    for (InstanceFilter filter : filters) {
      filtered = filter.filter(targetServiceId, request, filtered);
      if (filtered.isEmpty()) {
        return Mono.empty();
      }
    }

    for (InstanceSelector selector : selectors) {
      Optional<ServiceInstance> selected = selector.select(targetServiceId, request, filtered);
      if (selected.isPresent()) {
        return Mono.fromSupplier(() -> (Response<ServiceInstance>) new DefaultResponse(selected.get()));
      }
    }

    return Mono.empty();
  }
}
