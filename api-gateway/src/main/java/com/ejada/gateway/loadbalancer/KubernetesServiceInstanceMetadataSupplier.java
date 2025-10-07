package com.ejada.gateway.loadbalancer;

import com.ejada.gateway.config.GatewayKubernetesDiscoveryProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

/**
 * Decorates a {@link ServiceInstanceListSupplier} with Kubernetes pod metadata so that the
 * {@link TenantAffinityLoadBalancer} can make health-aware routing decisions when running
 * inside Kubernetes. The decorator keeps a lightweight cache of pod metadata to avoid
 * repeatedly hitting the Kubernetes API.
 */
public class KubernetesServiceInstanceMetadataSupplier implements ServiceInstanceListSupplier {

  private final ServiceInstanceListSupplier delegate;
  private final KubernetesPodMetadataProvider metadataProvider;
  private final GatewayKubernetesDiscoveryProperties properties;
  private final ConcurrentMap<String, CachedMetadata> cache = new ConcurrentHashMap<>();

  public KubernetesServiceInstanceMetadataSupplier(ServiceInstanceListSupplier delegate,
      KubernetesPodMetadataProvider metadataProvider,
      GatewayKubernetesDiscoveryProperties properties) {
    this.delegate = Objects.requireNonNull(delegate, "delegate");
    this.metadataProvider = Objects.requireNonNull(metadataProvider, "metadataProvider");
    this.properties = Objects.requireNonNull(properties, "properties");
  }

  @Override
  public Flux<List<ServiceInstance>> get() {
    return delegate.get().map(this::enrichInstances);
  }

  @Override
  @SuppressWarnings("rawtypes")
  public Flux<List<ServiceInstance>> get(Request request) {
    return delegate.get(request).map(this::enrichInstances);
  }

  @Override
  public String getServiceId() {
    return delegate.getServiceId();
  }

  private List<ServiceInstance> enrichInstances(List<ServiceInstance> instances) {
    if (!properties.isEnabled() || instances == null || instances.isEmpty()) {
      return instances;
    }
    List<ServiceInstance> enriched = new ArrayList<>(instances.size());
    for (ServiceInstance instance : instances) {
      enriched.add(enrichInstance(instance));
    }
    return enriched;
  }

  private ServiceInstance enrichInstance(ServiceInstance instance) {
    KubernetesPodMetadata metadata = resolveMetadata(instance);
    if (metadata == null) {
      return instance;
    }
    Map<String, String> merged = new LinkedHashMap<>(instance.getMetadata());
    if (StringUtils.hasText(metadata.availability())) {
      merged.putIfAbsent("availability", metadata.availability());
      merged.put("status", metadata.availability());
    }
    if (metadata.healthScore() != null) {
      merged.put("healthScore", Double.toString(metadata.healthScore()));
    }
    if (metadata.responseTimeMs() != null) {
      merged.put("avgResponseTimeMs", Double.toString(metadata.responseTimeMs()));
    }
    if (StringUtils.hasText(metadata.zone())) {
      merged.put("zone", metadata.zone());
      merged.put("spring-cloud-loadbalancer-zone", metadata.zone());
    }
    if (StringUtils.hasText(metadata.rolloutPhase())) {
      merged.put("rolloutPhase", metadata.rolloutPhase());
    }
    if (StringUtils.hasText(metadata.namespace())) {
      merged.putIfAbsent("k8s_namespace", metadata.namespace());
    }
    metadata.additionalMetadata().forEach(merged::putIfAbsent);
    return new ImmutableServiceInstance(instance, merged);
  }

  private KubernetesPodMetadata resolveMetadata(ServiceInstance instance) {
    String cacheKey = cacheKey(instance);
    CachedMetadata cached = cache.get(cacheKey);
    if (cached != null && !cached.isExpired()) {
      return cached.metadata();
    }
    Optional<KubernetesPodMetadata> resolved = metadataProvider.resolve(instance);
    if (resolved.isEmpty()) {
      cache.remove(cacheKey);
      return null;
    }
    KubernetesPodMetadata metadata = resolved.get();
    Duration ttl = properties.getMetadataTtl();
    Instant expiresAt = (ttl == null || ttl.isZero() || ttl.isNegative())
        ? null
        : Instant.now().plus(ttl);
    cache.put(cacheKey, new CachedMetadata(metadata, expiresAt));
    return metadata;
  }

  private String cacheKey(ServiceInstance instance) {
    return instance.getServiceId() + "|" + instance.getHost() + "|" + instance.getPort();
  }

  private record CachedMetadata(KubernetesPodMetadata metadata, Instant expiresAt) {

    boolean isExpired() {
      return expiresAt != null && Instant.now().isAfter(expiresAt);
    }
  }

  private static final class ImmutableServiceInstance implements ServiceInstance {

    private final ServiceInstance delegate;
    private final Map<String, String> metadata;

    private ImmutableServiceInstance(ServiceInstance delegate, Map<String, String> metadata) {
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

