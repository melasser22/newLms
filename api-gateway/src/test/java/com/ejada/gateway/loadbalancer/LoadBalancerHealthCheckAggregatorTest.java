package com.ejada.gateway.loadbalancer;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;

class LoadBalancerHealthCheckAggregatorTest {

  private final LoadBalancerHealthCheckAggregator aggregator =
      new LoadBalancerHealthCheckAggregator(Duration.ofMillis(200));

  @Test
  void updateShouldBlendMetadataIntoState() {
    ServiceInstance instance = serviceInstance("tenant-service-1", "tenant-service", 8080,
        Map.of(
            "healthScore", "0.8",
            "avgResponseTimeMs", "120",
            "zone", "europe-west",
            "availability", "UP"));

    LoadBalancerHealthCheckAggregator.InstanceState state = aggregator.update(instance);

    assertThat(state.getServiceId()).isEqualTo("tenant-service");
    assertThat(state.getInstanceId()).isEqualTo("tenant-service-1");
    assertThat(state.getZone()).isEqualTo("europe-west");
    assertThat(state.getHealthScore()).isEqualTo(0.8d);
    assertThat(state.getAverageResponseTimeMs()).isBetween(100d, 150d);
    assertThat(state.getEffectiveWeight()).isGreaterThan(0.3d);
  }

  @Test
  void recordResponseTimeShouldPenaliseSlowInstances() {
    ServiceInstance instance = serviceInstance("tenant-service-2", "tenant-service", 8081,
        Map.of(
            "healthScore", "1.0",
            "avgResponseTimeMs", "110",
            "zone", "us-east",
            "availability", "UP"));

    LoadBalancerHealthCheckAggregator.InstanceState baseline = aggregator.update(instance);
    aggregator.recordResponseTime(instance, Duration.ofMillis(1200));
    LoadBalancerHealthCheckAggregator.InstanceState updated = aggregator.snapshot("tenant-service").stream()
        .filter(state -> state.getInstanceId().equals("tenant-service-2"))
        .findFirst()
        .orElseThrow();

    assertThat(updated.getEffectiveWeight()).isLessThan(baseline.getEffectiveWeight());
    assertThat(updated.getAverageResponseTimeMs()).isGreaterThan(baseline.getAverageResponseTimeMs());
  }

  private ServiceInstance serviceInstance(String id, String serviceId, int port, Map<String, String> metadata) {
    Map<String, String> copy = new HashMap<>(metadata);
    DefaultServiceInstance instance = new DefaultServiceInstance(id, serviceId, "localhost", port, false, copy);
    return instance;
  }
}
