package com.ejada.gateway.loadbalancer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.client.loadbalancer.Request;
import reactor.core.publisher.Flux;

/**
 * Decorates the configured {@link ServiceInstanceListSupplier} to attach computed weights and zone
 * metadata from {@link LoadBalancerHealthCheckAggregator}. Downstream load balancers can use the
 * enriched metadata to influence routing decisions without duplicating the calculations.
 */
public class WeightedServiceInstanceListSupplier implements ServiceInstanceListSupplier {

  public static final String METADATA_WEIGHT = "gateway.lb.weight";
  public static final String METADATA_ZONE = "gateway.lb.zone";
  public static final String METADATA_AVAILABILITY = "gateway.lb.availability";
  public static final String METADATA_HEALTH = "gateway.lb.health-score";
  public static final String METADATA_RESPONSE_TIME = "gateway.lb.response-time";

  private final LoadBalancerHealthCheckAggregator aggregator;
  private final ServiceInstanceListSupplier delegate;

  public WeightedServiceInstanceListSupplier(ServiceInstanceListSupplier delegate,
      LoadBalancerHealthCheckAggregator aggregator) {
    this.delegate = Objects.requireNonNull(delegate, "delegate");
    this.aggregator = Objects.requireNonNull(aggregator, "aggregator");
  }

  @Override
  public Flux<List<ServiceInstance>> get() {
    return delegate.get().map(this::enrichInstances);
  }

  @SuppressWarnings("rawtypes")
  public Flux<List<ServiceInstance>> get(Request request) {
    return delegate.get(request).map(this::enrichInstances);
  }

  @Override
  public String getServiceId() {
    return delegate.getServiceId();
  }

  private List<ServiceInstance> enrichInstances(List<ServiceInstance> instances) {
    List<ServiceInstance> result = new ArrayList<>(instances.size());
    for (ServiceInstance instance : instances) {
      LoadBalancerHealthCheckAggregator.InstanceState state = aggregator.update(instance);
      Map<String, String> metadata = new LinkedHashMap<>(instance.getMetadata());
      metadata.put(METADATA_WEIGHT, Double.toString(state.getEffectiveWeight()));
      metadata.put(METADATA_ZONE, state.getZone());
      metadata.put(METADATA_AVAILABILITY, state.getAvailability().name());
      metadata.put(METADATA_HEALTH, Double.toString(state.getHealthScore()));
      metadata.put(METADATA_RESPONSE_TIME, Double.toString(state.getAverageResponseTimeMs()));
      result.add(new MetadataServiceInstance(instance, metadata));
    }
    return result;
  }

  private static final class MetadataServiceInstance implements ServiceInstance {

    private final ServiceInstance delegate;
    private final Map<String, String> metadata;

    private MetadataServiceInstance(ServiceInstance delegate, Map<String, String> metadata) {
      this.delegate = delegate;
      this.metadata = Map.copyOf(metadata);
    }

    @Override
    public String getInstanceId() {
      return delegate.getInstanceId();
    }

    @Override
    public String getServiceId() {
      return delegate.getServiceId();
    }

    @Override
    public String getHost() {
      return delegate.getHost();
    }

    @Override
    public int getPort() {
      return delegate.getPort();
    }

    @Override
    public boolean isSecure() {
      return delegate.isSecure();
    }

    @Override
    public java.net.URI getUri() {
      return delegate.getUri();
    }

    @Override
    public Map<String, String> getMetadata() {
      return metadata;
    }

    @Override
    public String getScheme() {
      return delegate.getScheme();
    }
  }
}
