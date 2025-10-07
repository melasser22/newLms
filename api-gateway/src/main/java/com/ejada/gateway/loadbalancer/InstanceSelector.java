package com.ejada.gateway.loadbalancer;

import java.util.List;
import java.util.Optional;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.Request;

/**
 * Strategy capable of selecting a single {@link ServiceInstance} from the filtered candidates.
 */
public interface InstanceSelector {

  Optional<ServiceInstance> select(String serviceId, Request<?> request, List<InstanceCandidate> candidates);
}
