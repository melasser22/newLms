package com.ejada.gateway.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Kubernetes-based service discovery.
 *
 * <p>The properties control how pod metadata is projected into Spring Cloud LoadBalancer
 * so the custom tenant-aware load balancer can make informed, health-aware routing
 * decisions. The defaults favor a single-namespace deployment that relies on standard
 * Kubernetes labels (such as {@code topology.kubernetes.io/zone}) and optional
 * annotations for custom rollout and health information.</p>
 */
@ConfigurationProperties(prefix = "gateway.kubernetes.discovery")
public class GatewayKubernetesDiscoveryProperties {

  /** Enables the Kubernetes discovery enhancements. */
  private boolean enabled = true;

  /** Overrides the namespace used when looking up pods (defaults to client namespace). */
  private String namespace;

  /** How long pod metadata is cached before refreshing from the Kubernetes API. */
  private Duration metadataTtl = Duration.ofSeconds(30);

  /**
   * Default response time (in milliseconds) used when pods do not expose a custom metric
   * via annotations.
   */
  private Duration defaultResponseTime = Duration.ofMillis(250);

  /** Primary pod label used to determine the pod's zone / topology. */
  private String zoneLabel = "topology.kubernetes.io/zone";

  /** Fallback pod labels used to determine the pod's zone / topology. */
  private List<String> zoneLabelFallbacks = new ArrayList<>(List.of(
      "failure-domain.beta.kubernetes.io/zone"));

  /** Service instance metadata keys that may already contain zone information. */
  private List<String> serviceZoneMetadataKeys = new ArrayList<>(List.of(
      "spring-cloud-loadbalancer-zone",
      "zone",
      "zoneId",
      "zone-id"));

  /** Pod annotation that optionally provides a rollout/deployment phase indicator. */
  private String rolloutAnnotation = "gateway.lb/rollout-phase";

  /** Pod label that optionally provides a rollout/deployment phase indicator. */
  private String rolloutLabel = "deployment.ejada.com/phase";

  /** Pod annotation providing a pre-computed health score for the instance. */
  private String healthScoreAnnotation = "gateway.lb/health-score";

  /** Pod annotation providing an observed average response time in milliseconds. */
  private String responseTimeAnnotation = "gateway.lb/avg-response-time-ms";

  /**
   * Pod annotation that can explicitly advertise availability state (e.g. draining,
   * out-of-service). When absent, readiness is used.
   */
  private String availabilityAnnotation = "gateway.lb/availability";

  /** Metadata keys that may contain the Kubernetes namespace for the instance. */
  private List<String> namespaceMetadataKeys = new ArrayList<>(List.of(
      "k8s_namespace",
      "kubernetes_namespace",
      "spring.cloud.kubernetes.namespace",
      "namespace"));

  /** Metadata keys that may contain the Kubernetes pod name for the instance. */
  private List<String> podNameMetadataKeys = new ArrayList<>(List.of(
      "k8s_pod_name",
      "podName",
      "pod_name",
      "kubernetes.io/pod-name"));

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  public Duration getMetadataTtl() {
    return metadataTtl;
  }

  public void setMetadataTtl(Duration metadataTtl) {
    this.metadataTtl = metadataTtl;
  }

  public Duration getDefaultResponseTime() {
    return defaultResponseTime;
  }

  public void setDefaultResponseTime(Duration defaultResponseTime) {
    this.defaultResponseTime = defaultResponseTime;
  }

  public String getZoneLabel() {
    return zoneLabel;
  }

  public void setZoneLabel(String zoneLabel) {
    this.zoneLabel = zoneLabel;
  }

  public List<String> getZoneLabelFallbacks() {
    return zoneLabelFallbacks;
  }

  public void setZoneLabelFallbacks(List<String> zoneLabelFallbacks) {
    this.zoneLabelFallbacks = zoneLabelFallbacks != null
        ? new ArrayList<>(zoneLabelFallbacks)
        : new ArrayList<>();
  }

  public List<String> getServiceZoneMetadataKeys() {
    return serviceZoneMetadataKeys;
  }

  public void setServiceZoneMetadataKeys(List<String> serviceZoneMetadataKeys) {
    this.serviceZoneMetadataKeys = serviceZoneMetadataKeys != null
        ? new ArrayList<>(serviceZoneMetadataKeys)
        : new ArrayList<>();
  }

  public String getRolloutAnnotation() {
    return rolloutAnnotation;
  }

  public void setRolloutAnnotation(String rolloutAnnotation) {
    this.rolloutAnnotation = rolloutAnnotation;
  }

  public String getRolloutLabel() {
    return rolloutLabel;
  }

  public void setRolloutLabel(String rolloutLabel) {
    this.rolloutLabel = rolloutLabel;
  }

  public String getHealthScoreAnnotation() {
    return healthScoreAnnotation;
  }

  public void setHealthScoreAnnotation(String healthScoreAnnotation) {
    this.healthScoreAnnotation = healthScoreAnnotation;
  }

  public String getResponseTimeAnnotation() {
    return responseTimeAnnotation;
  }

  public void setResponseTimeAnnotation(String responseTimeAnnotation) {
    this.responseTimeAnnotation = responseTimeAnnotation;
  }

  public String getAvailabilityAnnotation() {
    return availabilityAnnotation;
  }

  public void setAvailabilityAnnotation(String availabilityAnnotation) {
    this.availabilityAnnotation = availabilityAnnotation;
  }

  public List<String> getNamespaceMetadataKeys() {
    return namespaceMetadataKeys;
  }

  public void setNamespaceMetadataKeys(List<String> namespaceMetadataKeys) {
    this.namespaceMetadataKeys = namespaceMetadataKeys != null
        ? new ArrayList<>(namespaceMetadataKeys)
        : new ArrayList<>();
  }

  public List<String> getPodNameMetadataKeys() {
    return podNameMetadataKeys;
  }

  public void setPodNameMetadataKeys(List<String> podNameMetadataKeys) {
    this.podNameMetadataKeys = podNameMetadataKeys != null
        ? new ArrayList<>(podNameMetadataKeys)
        : new ArrayList<>();
  }
}

