package com.ejada.gateway.loadbalancer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.util.StringUtils;

/**
 * If a local zone is configured, restricts the candidate list to instances from the same zone when
 * possible.
 */
public class ZonePreferenceFilter implements InstanceFilter {

  private final String localZone;

  public ZonePreferenceFilter(String localZone) {
    this.localZone = (localZone != null) ? localZone.trim() : null;
  }

  @Override
  public List<InstanceCandidate> filter(String serviceId, Request<?> request, List<InstanceCandidate> candidates) {
    if (!StringUtils.hasText(localZone)) {
      return candidates;
    }

    List<InstanceCandidate> sameZone = new ArrayList<>();
    String normalized = localZone.toLowerCase(Locale.ROOT);
    for (InstanceCandidate candidate : candidates) {
      String zone = candidate.state().getZone();
      if (zone != null && zone.toLowerCase(Locale.ROOT).equals(normalized)) {
        sameZone.add(candidate);
      }
    }
    return sameZone.isEmpty() ? candidates : sameZone;
  }
}
