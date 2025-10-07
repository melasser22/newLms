package com.ejada.gateway.loadbalancer;

import java.util.Optional;
import org.springframework.cloud.client.ServiceInstance;

/**
 * Strategy interface that resolves {@link KubernetesPodMetadata} for a given
 * {@link ServiceInstance}. The default implementation uses the Fabric8
 * {@code KubernetesClient}, but the abstraction keeps the
 * {@link KubernetesServiceInstanceMetadataSupplier} testable.
 */
public interface KubernetesPodMetadataProvider {

  Optional<KubernetesPodMetadata> resolve(ServiceInstance instance);
}

