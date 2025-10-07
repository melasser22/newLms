package com.ejada.gateway.loadbalancer;

import static org.assertj.core.api.Assertions.assertThat;

import com.ejada.gateway.config.GatewayKubernetesDiscoveryProperties;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import reactor.core.publisher.Flux;

class KubernetesServiceInstanceMetadataSupplierTest {

  @Test
  void enrichesInstancesWithKubernetesMetadata() {
    GatewayKubernetesDiscoveryProperties properties = new GatewayKubernetesDiscoveryProperties();
    properties.setEnabled(true);
    properties.setMetadataTtl(Duration.ofSeconds(60));

    Map<String, String> metadata = new HashMap<>();
    metadata.put("k8s_namespace", "lms");
    ServiceInstance instance = new DefaultServiceInstance("tenant-service-1", "tenant-service",
        "10.0.0.5", 8080, false, metadata);

    KubernetesPodMetadata podMetadata = new KubernetesPodMetadata(
        "lms",
        "UP",
        0.9d,
        150d,
        "me-south-1a",
        "canary",
        Map.of("custom", "value"));

    AtomicInteger invocationCounter = new AtomicInteger();
    KubernetesPodMetadataProvider provider = svc -> {
      invocationCounter.incrementAndGet();
      return Optional.of(podMetadata);
    };

    ServiceInstanceListSupplier delegate = new StaticServiceInstanceListSupplier("tenant-service", List.of(instance));
    KubernetesServiceInstanceMetadataSupplier supplier = new KubernetesServiceInstanceMetadataSupplier(delegate, provider,
        properties);

    List<ServiceInstance> enriched = supplier.get().blockFirst();
    assertThat(enriched).singleElement().satisfies(enrichedInstance -> {
      assertThat(enrichedInstance.getMetadata()).containsEntry("status", "UP");
      assertThat(enrichedInstance.getMetadata()).containsEntry("zone", "me-south-1a");
      assertThat(enrichedInstance.getMetadata()).containsEntry("spring-cloud-loadbalancer-zone", "me-south-1a");
      assertThat(enrichedInstance.getMetadata()).containsEntry("rolloutPhase", "canary");
      assertThat(enrichedInstance.getMetadata()).containsEntry("healthScore", "0.9");
      assertThat(enrichedInstance.getMetadata()).containsEntry("avgResponseTimeMs", "150.0");
      assertThat(enrichedInstance.getMetadata()).containsEntry("custom", "value");
    });

    // Resolve again to ensure metadata is cached and provider is not invoked repeatedly.
    supplier.get().blockFirst();
    assertThat(invocationCounter).hasValue(1);
  }

  @Test
  void returnsOriginalInstanceWhenMetadataUnavailable() {
    GatewayKubernetesDiscoveryProperties properties = new GatewayKubernetesDiscoveryProperties();
    properties.setEnabled(true);
    properties.setMetadataTtl(Duration.ofSeconds(10));

    ServiceInstance instance = new DefaultServiceInstance("tenant-service-1", "tenant-service",
        "10.0.0.5", 8080, false, Map.of());

    KubernetesPodMetadataProvider provider = svc -> Optional.empty();

    ServiceInstanceListSupplier delegate = new StaticServiceInstanceListSupplier("tenant-service", List.of(instance));
    KubernetesServiceInstanceMetadataSupplier supplier = new KubernetesServiceInstanceMetadataSupplier(delegate, provider,
        properties);

    List<ServiceInstance> enriched = supplier.get().blockFirst();
    assertThat(enriched).singleElement().isSameAs(instance);
  }

  private static final class StaticServiceInstanceListSupplier implements ServiceInstanceListSupplier {

    private final String serviceId;
    private final List<ServiceInstance> instances;

    private StaticServiceInstanceListSupplier(String serviceId, List<ServiceInstance> instances) {
      this.serviceId = serviceId;
      this.instances = List.copyOf(instances);
    }

    @Override
    public Flux<List<ServiceInstance>> get() {
      return Flux.just(instances);
    }

    @Override
    public Flux<List<ServiceInstance>> get(Request request) {
      return get();
    }

    @Override
    public String getServiceId() {
      return serviceId;
    }
  }
}

