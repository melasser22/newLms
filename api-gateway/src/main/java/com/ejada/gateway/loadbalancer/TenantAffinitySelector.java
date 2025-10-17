package com.ejada.gateway.loadbalancer;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.RequestData;
import org.springframework.util.StringUtils;

/**
 * Applies rendezvous hashing to keep tenant requests anchored to the same service instance whenever
 * possible. When handling WebSocket upgrade requests, stickiness is maintained using the
 * {@link WebSocketStickTable}.
 */
public class TenantAffinitySelector implements InstanceSelector {

  private final TenantContext tenantContext;
  private final WebSocketStickTable stickTable;

  public TenantAffinitySelector(TenantContext tenantContext, WebSocketStickTable stickTable) {
    this.tenantContext = tenantContext;
    this.stickTable = stickTable;
  }

  @Override
  public Optional<ServiceInstance> select(String serviceId, Request<?> request, List<InstanceCandidate> candidates) {
    if (candidates.isEmpty()) {
      return Optional.empty();
    }
    RequestData requestData = LoadBalancerRequestAdapter.resolveRequestData(request);
    String tenantId = LoadBalancerRequestAdapter.resolveTenantId(request, tenantContext);
    boolean websocket = LoadBalancerRequestAdapter.isWebSocket(requestData);
    String stickinessKey = websocket ? LoadBalancerRequestAdapter.resolveStickinessKey(requestData, tenantId) : null;
    String affinityKey = StringUtils.hasText(tenantId) ? tenantId : stickinessKey;

    if (stickinessKey != null && stickTable != null) {
      Optional<ServiceInstance> sticky = stickTable.lookup(stickinessKey, serviceId,
          candidates.stream().map(InstanceCandidate::instance).toList());
      if (sticky.isPresent()) {
        return sticky;
      }
    }

    if (!StringUtils.hasText(affinityKey)) {
      return Optional.empty();
    }

    ServiceInstance chosen = selectByRendezvous(affinityKey, candidates);
    if (chosen != null && stickinessKey != null && stickTable != null) {
      stickTable.record(stickinessKey, chosen);
    }
    return Optional.ofNullable(chosen);
  }

  private ServiceInstance selectByRendezvous(String key, List<InstanceCandidate> candidates) {
    double bestScore = Double.NEGATIVE_INFINITY;
    InstanceCandidate best = null;
    for (InstanceCandidate candidate : candidates) {
      double weight = Math.max(0d, candidate.state().getEffectiveWeight());
      if (weight <= 0d) {
        continue;
      }
      double hash = unitHash(key, candidate.state().getInstanceId());
      double score = weight / -Math.log(hash == 0d ? Double.MIN_VALUE : hash);
      if (score > bestScore) {
        bestScore = score;
        best = candidate;
      }
    }
    return best != null ? best.instance() : null;
  }

  private double unitHash(String tenantKey, String instanceId) {
    byte[] data = (tenantKey + '|' + instanceId).getBytes(StandardCharsets.UTF_8);
    long hash = 1469598103934665603L;
    for (byte b : data) {
      hash ^= (b & 0xff);
      hash *= 1099511628211L;
    }
    long bits = (hash >>> 11) | 0x3ff0000000000000L;
    double value = Double.longBitsToDouble(bits) - 1.0d;
    if (value <= 0d || value >= 1d) {
      return 0.5d;
    }
    return value;
  }
}
