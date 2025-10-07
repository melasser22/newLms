package com.ejada.gateway.loadbalancer;

import com.ejada.gateway.loadbalancer.LoadBalancerHealthCheckAggregator.Availability;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.cloud.client.loadbalancer.Request;

/**
 * Removes instances that are marked {@link Availability#DOWN} or whose computed weight is zero.
 */
public class HealthAwareFilter implements InstanceFilter {

  @Override
  public List<InstanceCandidate> filter(String serviceId, Request<?> request, List<InstanceCandidate> candidates) {
    return candidates.stream()
        .filter(candidate -> candidate.state().getAvailability() != Availability.DOWN)
        .filter(candidate -> candidate.state().getEffectiveWeight() > 0d)
        .collect(Collectors.toList());
  }
}
