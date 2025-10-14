package com.ejada.gateway.loadbalancer;

import com.ejada.gateway.config.TenantMigrationProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import reactor.core.publisher.Mono;

/**
 * Resolves tenant specific migration targets and delegates selection to an
 * internal {@link CompositeLoadBalancer}-style pipeline for the alternative
 * service identifier.
 */
public class TenantMigrationService {

  private static final Logger log = LoggerFactory.getLogger(TenantMigrationService.class);

  private final TenantMigrationProperties properties;
  private final LoadBalancerClientFactory clientFactory;
  private final LoadBalancerHealthCheckAggregator aggregator;

  public TenantMigrationService(TenantMigrationProperties properties,
                                LoadBalancerClientFactory clientFactory,
                                LoadBalancerHealthCheckAggregator aggregator) {
    this.properties = properties;
    this.clientFactory = clientFactory;
    this.aggregator = aggregator;
  }

  public Optional<String> resolveTarget(String serviceId, String tenantId) {
    return properties.resolveTargetServiceId(serviceId, tenantId);
  }

  public Mono<ResponseWrapper> chooseFromTarget(String targetServiceId,
                                                Request<?> request,
                                                List<InstanceFilter> filters,
                                                List<InstanceSelector> selectors) {
    ObjectProvider<ServiceInstanceListSupplier> provider =
        clientFactory.getLazyProvider(targetServiceId, ServiceInstanceListSupplier.class);
    ServiceInstanceListSupplier supplier = provider.getIfAvailable();
    if (supplier == null) {
      log.warn("No ServiceInstanceListSupplier available for migrated service {}", targetServiceId);
      return Mono.empty();
    }
    return supplier.get(request)
        .next()
        .flatMap(instances -> select(targetServiceId, request, instances, filters, selectors))
        .switchIfEmpty(Mono.empty());
  }

  private Mono<ResponseWrapper> select(String serviceId,
                                       Request<?> request,
                                       List<ServiceInstance> instances,
                                       List<InstanceFilter> filters,
                                       List<InstanceSelector> selectors) {
    if (instances == null || instances.isEmpty()) {
      return Mono.empty();
    }
    List<InstanceCandidate> candidates = new ArrayList<>(instances.size());
    for (ServiceInstance instance : instances) {
      candidates.add(new InstanceCandidate(instance, aggregator.update(instance)));
    }
    List<InstanceCandidate> filtered = candidates;
    for (InstanceFilter filter : filters) {
      filtered = filter.filter(serviceId, request, filtered);
      if (filtered.isEmpty()) {
        return Mono.empty();
      }
    }
    for (InstanceSelector selector : selectors) {
      Optional<ServiceInstance> selected = selector.select(serviceId, request, filtered);
      if (selected.isPresent()) {
        return Mono.just(new ResponseWrapper(selected.get()));
      }
    }
    return Mono.empty();
  }

  /** Simple wrapper mirroring Spring's {@link org.springframework.cloud.client.loadbalancer.Response}. */
  public static final class ResponseWrapper {
    private final ServiceInstance instance;

    public ResponseWrapper(ServiceInstance instance) {
      this.instance = instance;
    }

    public ServiceInstance instance() {
      return instance;
    }
  }
}

