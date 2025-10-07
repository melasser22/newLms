package com.ejada.gateway.loadbalancer;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.util.StringUtils;

/**
 * Implements the "smooth" weighted round-robin algorithm popularised by Nginx. Each instance keeps
 * a running score that is increased by its weight on every selection cycle and decreased by the
 * total weight whenever it is chosen. This guarantees even distribution proportional to the
 * effective weight while avoiding starvation.
 */
public class WeightedRoundRobinSelector implements InstanceSelector {

  private final ConcurrentMap<String, ConcurrentMap<String, SmoothEntry>> perServiceState = new ConcurrentHashMap<>();

  @Override
  public Optional<ServiceInstance> select(String serviceId, Request<?> request, List<InstanceCandidate> candidates) {
    if (candidates.isEmpty()) {
      return Optional.empty();
    }
    ConcurrentMap<String, SmoothEntry> state = perServiceState.computeIfAbsent(serviceId, key -> new ConcurrentHashMap<>());
    synchronized (state) {
      double totalWeight = 0d;
      SmoothEntry best = null;
      ServiceInstance bestInstance = null;
      Set<String> active = new HashSet<>();

      for (InstanceCandidate candidate : candidates) {
        double weight = Math.max(0d, candidate.state().getEffectiveWeight());
        String instanceId = candidate.state().getInstanceId();
        if (!StringUtils.hasText(instanceId) || weight <= 0d) {
          if (StringUtils.hasText(instanceId)) {
            state.remove(instanceId);
          }
          continue;
        }
        active.add(instanceId);
        SmoothEntry entry = state.computeIfAbsent(instanceId, id -> new SmoothEntry());
        entry.currentWeight += weight;
        totalWeight += weight;
        if (best == null || entry.currentWeight > best.currentWeight) {
          best = entry;
          bestInstance = candidate.instance();
        }
      }

      state.keySet().retainAll(active);

      if (best == null || bestInstance == null || totalWeight <= 0d) {
        return Optional.empty();
      }

      best.currentWeight -= totalWeight;
      return Optional.of(bestInstance);
    }
  }

  private static final class SmoothEntry {
    private double currentWeight;
  }
}
