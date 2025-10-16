package com.ejada.gateway.loadbalancer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.ejada.gateway.config.TenantMigrationProperties;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import reactor.core.publisher.Flux;

@ExtendWith(MockitoExtension.class)
class TenantMigrationServiceTest {

    @Mock
    private LoadBalancerClientFactory clientFactory;

    @Mock
    private ObjectProvider<ServiceInstanceListSupplier> supplierProvider;

    private final LoadBalancerHealthCheckAggregator aggregator = new LoadBalancerHealthCheckAggregator();

    @Test
    void resolvesTargetServiceForTenant() {
        TenantMigrationProperties properties = new TenantMigrationProperties();
        TenantMigrationProperties.ServiceMigration migration = new TenantMigrationProperties.ServiceMigration();
        migration.setTenants(Map.of("tenant-a", "catalog-v2"));
        properties.getServices().put("catalog", migration);

        TenantMigrationService service = new TenantMigrationService(properties, clientFactory, aggregator);

        assertThat(service.resolveTarget("catalog", "tenant-a")).contains("catalog-v2");
        assertThat(service.resolveTarget("catalog", "tenant-b")).isEmpty();
    }

    @Test
    void chooseFromTargetUsesMigratedInstances() {
        TenantMigrationProperties properties = new TenantMigrationProperties();
        TenantMigrationProperties.ServiceMigration migration = new TenantMigrationProperties.ServiceMigration();
        migration.setTenants(Map.of("tenant-a", "catalog-v2"));
        properties.getServices().put("catalog", migration);

        ServiceInstance instance = new DefaultServiceInstance("catalog-v2-1", "catalog-v2", "localhost", 8080, false,
            Map.of());
        ServiceInstanceListSupplier supplier = new StaticSupplier("catalog-v2", List.of(instance));

        when(clientFactory.getLazyProvider("catalog-v2", ServiceInstanceListSupplier.class))
            .thenReturn(supplierProvider);
        when(supplierProvider.getIfAvailable()).thenReturn(supplier);

        TenantMigrationService service = new TenantMigrationService(properties, clientFactory, aggregator);

        TenantMigrationService.ResponseWrapper wrapper = service.chooseFromTarget(
                "catalog-v2",
                new SimpleRequest(),
                List.of((InstanceFilter) (svc, req, candidates) -> candidates),
                List.of((InstanceSelector) (svc, req, candidates) ->
                        candidates.isEmpty() ? Optional.empty() : Optional.of(candidates.get(0).instance())))
            .block();

        assertThat(wrapper).isNotNull();
        assertThat(wrapper.instance()).isSameAs(instance);
    }

    private static final class StaticSupplier implements ServiceInstanceListSupplier {

        private final String serviceId;
        private final List<ServiceInstance> instances;

        private StaticSupplier(String serviceId, List<ServiceInstance> instances) {
            this.serviceId = serviceId;
            this.instances = instances;
        }

        @Override
        public String getServiceId() {
            return serviceId;
        }

        @Override
        public Flux<List<ServiceInstance>> get() {
            return Flux.just(instances);
        }

        @Override
        public Flux<List<ServiceInstance>> get(Request<?> request) {
            return Flux.just(instances);
        }
    }

    private static final class SimpleRequest implements Request<Object> {
        @Override
        public Object getContext() {
            return null;
        }
    }
}
