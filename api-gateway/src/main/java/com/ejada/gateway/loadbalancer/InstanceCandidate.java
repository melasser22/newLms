package com.ejada.gateway.loadbalancer;

import com.ejada.gateway.loadbalancer.LoadBalancerHealthCheckAggregator.InstanceState;
import org.springframework.cloud.client.ServiceInstance;

/**
 * Immutable view of a {@link ServiceInstance} paired with its computed {@link InstanceState}.
 */
public record InstanceCandidate(ServiceInstance instance, InstanceState state) {

  public InstanceCandidate {
    if (instance == null) {
      throw new IllegalArgumentException("instance must not be null");
    }
    if (state == null) {
      throw new IllegalArgumentException("state must not be null");
    }
  }
}
