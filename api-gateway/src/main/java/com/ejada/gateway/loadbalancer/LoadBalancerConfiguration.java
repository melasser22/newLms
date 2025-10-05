package com.ejada.gateway.loadbalancer;

import com.ejada.gateway.config.GatewayRoutesProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

/**
 * Central configuration that replaces the default round-robin load balancer with the
 * {@link TenantAffinityLoadBalancer} and ensures service instances are enriched with dynamic
 * weighting metadata.
 */
@Configuration
@LoadBalancerClients
public class LoadBalancerConfiguration {

  @Bean
  public LoadBalancerHealthCheckAggregator loadBalancerHealthCheckAggregator() {
    return new LoadBalancerHealthCheckAggregator();
  }

  @Bean
  public WebSocketStickTable webSocketStickTable(
      @Value("${gateway.loadbalancer.websocket-stickiness-ttl:PT15M}") java.time.Duration ttl) {
    return new WebSocketStickTable(ttl);
  }

  @Bean
  @Primary
  public ServiceInstanceListSupplier serviceInstanceListSupplier(ConfigurableApplicationContext context,
      LoadBalancerHealthCheckAggregator aggregator) {
    ServiceInstanceListSupplier delegate = ServiceInstanceListSupplier.builder()
        .withDiscoveryClient()
        .withCaching()
        .build(context);
    return new WeightedServiceInstanceListSupplier(delegate, aggregator);
  }

  @Bean
  public ReactorServiceInstanceLoadBalancer reactorServiceInstanceLoadBalancer(
      LoadBalancerClientFactory clientFactory,
      LoadBalancerHealthCheckAggregator aggregator,
      GatewayRoutesProperties routesProperties,
      WebSocketStickTable stickTable,
      @Value("${spring.cloud.loadbalancer.zone:}") String localZone,
      Environment environment) {
    String serviceId = LoadBalancerClientFactory.getName(environment);
    ObjectProvider<ServiceInstanceListSupplier> provider = clientFactory
        .getLazyProvider(serviceId, ServiceInstanceListSupplier.class);
    return new TenantAffinityLoadBalancer(serviceId, provider, aggregator, routesProperties, stickTable, localZone);
  }
}
