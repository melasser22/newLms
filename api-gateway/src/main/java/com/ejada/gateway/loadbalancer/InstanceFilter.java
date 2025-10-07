package com.ejada.gateway.loadbalancer;

import java.util.List;
import org.springframework.cloud.client.loadbalancer.Request;

/**
 * Filters the list of service instance candidates before a selector chooses the final instance.
 */
public interface InstanceFilter {

  List<InstanceCandidate> filter(String serviceId, Request<?> request, List<InstanceCandidate> candidates);
}
