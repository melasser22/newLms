package com.ejada.gateway.loadbalancer;

import static org.assertj.core.api.Assertions.assertThat;

import com.ejada.gateway.config.GatewayRoutesProperties;
import com.ejada.gateway.config.GatewayRoutesProperties.ServiceRoute;
import com.ejada.gateway.config.GatewayRoutesProperties.ServiceRoute.LoadBalancingStrategy;
import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultRequest;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.RequestData;
import org.springframework.cloud.client.loadbalancer.RequestDataContext;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.LinkedMultiValueMap;

class TenantAffinityLoadBalancerTest {

  private GatewayRoutesProperties routesProperties;
  private LoadBalancerHealthCheckAggregator aggregator;
  private TenantAffinityLoadBalancer loadBalancer;
  private StaticServiceInstanceListSupplier baseSupplier;
  private WeightedServiceInstanceListSupplier weightedSupplier;

  @BeforeEach
  void setUp() {
    routesProperties = new GatewayRoutesProperties();
    ServiceRoute route = new ServiceRoute();
    route.setId("tenant-route");
    route.setUri(URI.create("lb://tenant-service"));
    route.setPaths(List.of("/"));
    route.setLbStrategy(LoadBalancingStrategy.WEIGHTED_RESPONSE_TIME);
    routesProperties.getRoutes().put("tenant", route);

    aggregator = new LoadBalancerHealthCheckAggregator(Duration.ofMillis(200));

    List<ServiceInstance> instances = List.of(
        instance("tenant-service-1", "zone-a", 8080, 0.9, 90),
        instance("tenant-service-2", "zone-a", 8081, 0.8, 150),
        instance("tenant-service-3", "zone-b", 8082, 0.7, 200));

    baseSupplier = new StaticServiceInstanceListSupplier("tenant-service", instances);
    weightedSupplier = new WeightedServiceInstanceListSupplier(baseSupplier, aggregator);

    ObjectProvider<ServiceInstanceListSupplier> provider = new StaticObjectProvider<>(weightedSupplier);
    loadBalancer = new TenantAffinityLoadBalancer("tenant-service", provider, aggregator, routesProperties,
        new WebSocketStickTable(Duration.ofMinutes(5)), "zone-a");
  }

  @Test
  void chooseShouldBeConsistentForTenant() {
    Request<?> request = requestWithTenant("tenant-route", "tenant-alpha");

    Response<ServiceInstance> first = loadBalancer.choose(request).block();
    Response<ServiceInstance> second = loadBalancer.choose(request).block();

    assertThat(first).isNotNull();
    assertThat(first.getServer()).isNotNull();
    assertThat(second).isNotNull();
    assertThat(second.getServer()).isNotNull();
    assertThat(second.getServer().getInstanceId()).isEqualTo(first.getServer().getInstanceId());
  }

  @Test
  void websocketStickTableShouldReuseInstanceWithoutTenant() {
    Request<?> request = websocketRequest("tenant-route", "handshake-key-1");

    ServiceInstance first = loadBalancer.choose(request).map(Response::getServer).block();
    ServiceInstance second = loadBalancer.choose(request).map(Response::getServer).block();

    assertThat(first).isNotNull();
    assertThat(second).isNotNull();
    assertThat(second.getInstanceId()).isEqualTo(first.getInstanceId());
  }

  private ServiceInstance instance(String id, String zone, int port, double health, double responseTime) {
    Map<String, String> metadata = new HashMap<>();
    metadata.put("zone", zone);
    metadata.put("healthScore", Double.toString(health));
    metadata.put("avgResponseTimeMs", Double.toString(responseTime));
    metadata.put("availability", "UP");
    return new DefaultServiceInstance(id, "tenant-service", "localhost", port, false, metadata);
  }

  private Request<?> requestWithTenant(String routeId, String tenant) {
    HttpHeaders headers = new HttpHeaders();
    headers.add("X-Tenant-Id", tenant);
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, routeId);
    RequestData data = new RequestData(HttpMethod.GET, URI.create("http://gateway/test"), headers,
        new LinkedMultiValueMap<>(), attributes);
    RequestDataContext context = new RequestDataContext(data);
    return new DefaultRequest<>(context);
  }

  private Request<?> websocketRequest(String routeId, String websocketKey) {
    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.UPGRADE, "websocket");
    headers.add("Sec-WebSocket-Key", websocketKey);
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, routeId);
    RequestData data = new RequestData(HttpMethod.GET, URI.create("http://gateway/ws"), headers,
        new LinkedMultiValueMap<>(), attributes);
    RequestDataContext context = new RequestDataContext(data);
    return new DefaultRequest<>(context);
  }

  private static class StaticServiceInstanceListSupplier implements ServiceInstanceListSupplier {

    private final String serviceId;
    private final List<ServiceInstance> instances;

    private StaticServiceInstanceListSupplier(String serviceId, List<ServiceInstance> instances) {
      this.serviceId = serviceId;
      this.instances = List.copyOf(instances);
    }

    @Override
    public String getServiceId() {
      return serviceId;
    }

    @Override
    public reactor.core.publisher.Flux<List<ServiceInstance>> get() {
      return reactor.core.publisher.Flux.just(instances);
    }

    @Override
    public reactor.core.publisher.Flux<List<ServiceInstance>> get(Request request) {
      return get();
    }
  }

  private static class StaticObjectProvider<T> implements ObjectProvider<T> {

    private final T value;

    private StaticObjectProvider(T value) {
      this.value = value;
    }

    @Override
    public T getObject(Object... args) {
      return value;
    }

    @Override
    public T getIfAvailable() {
      return value;
    }

    @Override
    public T getIfAvailable(java.util.function.Supplier<T> supplier) {
      return value != null ? value : supplier.get();
    }

    @Override
    public T getIfUnique() {
      return value;
    }

    @Override
    public T getIfUnique(java.util.function.Supplier<T> supplier) {
      return value != null ? value : supplier.get();
    }

    @Override
    public T getObject() {
      return value;
    }

    @Override
    public java.util.stream.Stream<T> stream() {
      return value != null ? java.util.stream.Stream.of(value) : java.util.stream.Stream.empty();
    }

    @Override
    public java.util.stream.Stream<T> orderedStream() {
      return stream();
    }
  }
}
