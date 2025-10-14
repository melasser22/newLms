package com.ejada.gateway.loadbalancer;

import com.ejada.gateway.config.GatewayRoutesProperties;
import com.ejada.gateway.config.GatewayRoutesProperties.ServiceRoute;
import com.ejada.gateway.config.GatewayRoutesProperties.ServiceRoute.TenantRoutingRule;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.util.StringUtils;

/**
 * Applies tenant-specific weighting and dedicated instance routing before falling back to the
 * tenant affinity and weighted round-robin selectors.
 */
public class TenantWeightedSelector implements InstanceSelector {

  private final GatewayRoutesProperties routesProperties;
  private final TenantContext tenantContext;
  private final WeightedRoundRobinSelector delegate = new WeightedRoundRobinSelector();

  public TenantWeightedSelector(GatewayRoutesProperties routesProperties, TenantContext tenantContext) {
    this.routesProperties = Objects.requireNonNull(routesProperties, "routesProperties");
    this.tenantContext = tenantContext;
  }

  @Override
  public Optional<ServiceInstance> select(String serviceId, Request<?> request, List<InstanceCandidate> candidates) {
    if (candidates.isEmpty()) {
      return Optional.empty();
    }
    String tenantId = LoadBalancerRequestAdapter.resolveTenantId(request, tenantContext);
    if (!StringUtils.hasText(tenantId)) {
      return Optional.empty();
    }
    String routeId = LoadBalancerRequestAdapter.resolveRouteId(request);
    if (!StringUtils.hasText(routeId)) {
      return Optional.empty();
    }
    Optional<ServiceRoute> routeOpt = routesProperties.findRouteById(routeId);
    if (routeOpt.isEmpty()) {
      return Optional.empty();
    }
    TenantRoutingRule rule = routeOpt.get().getTenantRouting().get(tenantId);
    if (rule == null) {
      return Optional.empty();
    }
    List<InstanceCandidate> filtered = applyDedicatedInstances(rule, candidates);
    if (filtered.isEmpty()) {
      return Optional.empty();
    }
    Optional<ServiceInstance> weighted = applyTenantWeights(rule, filtered);
    if (weighted.isPresent()) {
      return weighted;
    }
    return delegate.select(serviceId, request, filtered);
  }

  private List<InstanceCandidate> applyDedicatedInstances(TenantRoutingRule rule, List<InstanceCandidate> candidates) {
    List<String> dedicated = rule.getDedicatedInstances();
    if (dedicated.isEmpty()) {
      return candidates;
    }
    List<InstanceCandidate> matched = new ArrayList<>();
    for (InstanceCandidate candidate : candidates) {
      String instanceId = candidate.state().getInstanceId();
      if (!StringUtils.hasText(instanceId)) {
        continue;
      }
      String normalized = instanceId.trim().toLowerCase(Locale.ROOT);
      if (dedicated.contains(normalized)) {
        matched.add(candidate);
      }
    }
    if (!matched.isEmpty()) {
      return matched;
    }
    return candidates;
  }

  private Optional<ServiceInstance> applyTenantWeights(TenantRoutingRule rule, List<InstanceCandidate> candidates) {
    Map<String, Integer> weights = rule.getInstanceWeights();
    if (weights.isEmpty()) {
      return Optional.empty();
    }
    List<WeightedEntry> entries = new ArrayList<>();
    double totalWeight = 0d;
    for (InstanceCandidate candidate : candidates) {
      double baseWeight = Math.max(0d, candidate.state().getEffectiveWeight());
      if (baseWeight <= 0d) {
        continue;
      }
      String instanceId = candidate.state().getInstanceId();
      if (!StringUtils.hasText(instanceId)) {
        continue;
      }
      String normalized = instanceId.trim();
      Integer override = weights.get(normalized);
      if (override == null) {
        override = weights.get(normalized.toLowerCase(Locale.ROOT));
      }
      double effectiveWeight = baseWeight;
      if (override != null && override > 0) {
        effectiveWeight = baseWeight * override;
      }
      if (effectiveWeight <= 0d) {
        continue;
      }
      totalWeight += effectiveWeight;
      entries.add(new WeightedEntry(candidate.instance(), effectiveWeight));
    }
    if (entries.isEmpty() || totalWeight <= 0d) {
      return Optional.empty();
    }
    double pick = ThreadLocalRandom.current().nextDouble(totalWeight);
    double cumulative = 0d;
    for (WeightedEntry entry : entries) {
      cumulative += entry.weight();
      if (pick <= cumulative) {
        return Optional.of(entry.instance());
      }
    }
    return Optional.of(entries.get(entries.size() - 1).instance());
  }

  private record WeightedEntry(ServiceInstance instance, double weight) {
  }
}
