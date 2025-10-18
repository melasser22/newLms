package com.ejada.gateway.loadbalancer;

import com.ejada.gateway.config.GatewayKubernetesDiscoveryProperties;
import com.ejada.gateway.config.GatewayRoutesProperties;
import com.ejada.gateway.config.TenantMigrationProperties;
import io.fabric8.kubernetes.client.KubernetesClient;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.core.RetryAwareServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * Central configuration that replaces the default round-robin load balancer with the
 * {@link CompositeLoadBalancer} and ensures service instances are enriched with dynamic
 * weighting metadata.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({GatewayKubernetesDiscoveryProperties.class, TenantMigrationProperties.class})
@LoadBalancerClients(defaultConfiguration = LoadBalancerConfiguration.TenantAffinityLoadBalancerClientConfiguration.class)
public class LoadBalancerConfiguration {

  @Bean
  public LoadBalancerHealthCheckAggregator loadBalancerHealthCheckAggregator() {
    return new LoadBalancerHealthCheckAggregator();
  }

  @Bean
  public TenantMigrationService tenantMigrationService(TenantMigrationProperties migrationProperties,
                                                       LoadBalancerClientFactory clientFactory,
                                                       LoadBalancerHealthCheckAggregator aggregator) {
    return new TenantMigrationService(migrationProperties, clientFactory, aggregator);
  }

  @Bean
  public WebSocketStickTable webSocketStickTable(
      @Value("${gateway.loadbalancer.websocket-stickiness-ttl:PT15M}") java.time.Duration ttl) {
    return new WebSocketStickTable(ttl);
  }

  @Bean
  public TenantContext tenantContext() {
    return new ContextManagerTenantContext();
  }

  @Configuration(proxyBeanMethods = false)
  static class TenantAffinityLoadBalancerClientConfiguration {

    @Bean
    public ServiceInstanceListSupplier baseDiscoveryClientServiceInstanceListSupplier(
        ConfigurableApplicationContext context) {
      return ServiceInstanceListSupplier.builder()
          .withDiscoveryClient()
          .withCaching()
          .build(context);
    }

    @Bean
    @Primary
    public ServiceInstanceListSupplier retryAwareDiscoveryClientServiceInstanceListSupplier(
        @Qualifier("baseDiscoveryClientServiceInstanceListSupplier")
        ServiceInstanceListSupplier discoveryDelegate,
        LoadBalancerHealthCheckAggregator aggregator,
        GatewayKubernetesDiscoveryProperties kubernetesDiscoveryProperties,
        ObjectProvider<KubernetesPodMetadataProvider> metadataProvider,
        ConfigurableApplicationContext context) {
      return new LazyServiceInstanceListSupplier(() -> {
        ServiceInstanceListSupplier delegate = ServiceInstanceListSupplier.builder()
            .withBlockingDiscoveryClient()
            .withCaching()
            .withRetryAwareness()
            .build(context);
        KubernetesPodMetadataProvider provider = metadataProvider.getIfAvailable();
        if (provider != null && kubernetesDiscoveryProperties.isEnabled()) {
          delegate = new KubernetesServiceInstanceMetadataSupplier(delegate, provider, kubernetesDiscoveryProperties);
        }
        return new WeightedServiceInstanceListSupplier(delegate, aggregator);
      });
    }

    @Bean
    @ConditionalOnProperty(name = LoadBalancerClientFactory.PROPERTY_NAME)
    public ReactorServiceInstanceLoadBalancer tenantAndHealthAwareLoadBalancer(
        LoadBalancerClientFactory clientFactory,
        LoadBalancerHealthCheckAggregator aggregator,
        GatewayRoutesProperties routesProperties,
        TenantMigrationService migrationService,
        WebSocketStickTable stickTable,
        @Value("${spring.cloud.loadbalancer.zone:}") String localZone,
        Environment environment,
        TenantContext tenantContext) {
      String serviceId = LoadBalancerClientFactory.getName(environment);
      if (!StringUtils.hasText(serviceId)) {
        throw new IllegalStateException(
            "No load-balancer client name configured; ensure requests use 'lb://<serviceId>' URIs.");
      }
      ObjectProvider<ServiceInstanceListSupplier> provider = clientFactory
          .getLazyProvider(serviceId, ServiceInstanceListSupplier.class);
      return new CompositeLoadBalancer(serviceId, provider, aggregator, routesProperties,
          migrationService,
          tenantContext,
          List.of(
              new HealthAwareFilter(),
              new ZonePreferenceFilter(localZone)),
          List.of(
              new TenantWeightedSelector(routesProperties, tenantContext),
              new TenantAffinitySelector(tenantContext, stickTable),
              new WeightedRoundRobinSelector()));
    }
  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass(KubernetesClient.class)
  static class KubernetesLoadBalancerMetadataConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(KubernetesClient.class)
    public KubernetesPodMetadataProvider kubernetesPodMetadataProvider(KubernetesClient client,
        GatewayKubernetesDiscoveryProperties properties) {
      return new Fabric8KubernetesPodMetadataProvider(client, properties);
    }
  }
}
