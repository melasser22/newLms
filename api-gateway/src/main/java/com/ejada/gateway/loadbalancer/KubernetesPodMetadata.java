package com.ejada.gateway.loadbalancer;

import java.util.Map;

/**
 * Normalised metadata extracted from a Kubernetes pod that is relevant to the
 * gateway's health-aware load-balancing decisions.
 */
public record KubernetesPodMetadata(
    String namespace,
    String availability,
    Double healthScore,
    Double responseTimeMs,
    String zone,
    String rolloutPhase,
    Map<String, String> additionalMetadata) {

  public KubernetesPodMetadata {
    additionalMetadata = additionalMetadata == null ? Map.of() : Map.copyOf(additionalMetadata);
  }
}

