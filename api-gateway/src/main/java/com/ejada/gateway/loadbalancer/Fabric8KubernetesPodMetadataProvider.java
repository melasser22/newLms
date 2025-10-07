package com.ejada.gateway.loadbalancer;

import com.ejada.gateway.config.GatewayKubernetesDiscoveryProperties;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodCondition;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * {@link KubernetesPodMetadataProvider} backed by the Fabric8
 * {@link KubernetesClient}. The provider looks up pods by name or IP address and
 * projects relevant labels/annotations into {@link KubernetesPodMetadata} so the
 * load balancer can honour pod readiness, rollout state and zone topology.
 */
public class Fabric8KubernetesPodMetadataProvider implements KubernetesPodMetadataProvider {

  private static final Log LOG = LogFactory.getLog(Fabric8KubernetesPodMetadataProvider.class);

  private final KubernetesClient client;
  private final GatewayKubernetesDiscoveryProperties properties;

  public Fabric8KubernetesPodMetadataProvider(KubernetesClient client,
      GatewayKubernetesDiscoveryProperties properties) {
    this.client = client;
    this.properties = properties;
  }

  @Override
  public Optional<KubernetesPodMetadata> resolve(ServiceInstance instance) {
    if (!properties.isEnabled() || instance == null) {
      return Optional.empty();
    }
    String namespace = resolveNamespace(instance);
    if (!StringUtils.hasText(namespace)) {
      return Optional.empty();
    }
    Pod pod = locatePod(instance, namespace);
    if (pod == null) {
      return Optional.empty();
    }
    return Optional.of(extractMetadata(namespace, pod, instance));
  }

  private String resolveNamespace(ServiceInstance instance) {
    if (StringUtils.hasText(properties.getNamespace())) {
      return properties.getNamespace();
    }
    for (String key : properties.getNamespaceMetadataKeys()) {
      String value = instance.getMetadata().get(key);
      if (StringUtils.hasText(value)) {
        return value.trim();
      }
    }
    String clientNamespace = client.getConfiguration() != null ? client.getConfiguration().getNamespace() : null;
    if (StringUtils.hasText(clientNamespace)) {
      return clientNamespace;
    }
    return "default";
  }

  private Pod locatePod(ServiceInstance instance, String namespace) {
    try {
      String podName = resolvePodName(instance);
      if (StringUtils.hasText(podName)) {
        Pod pod = client.pods().inNamespace(namespace).withName(podName).get();
        if (pod != null) {
          return pod;
        }
      }
      String ip = instance.getHost();
      if (!StringUtils.hasText(ip)) {
        return null;
      }
      PodList podList = client.pods().inNamespace(namespace)
          .withField("status.podIP", ip)
          .list();
      if (podList == null || CollectionUtils.isEmpty(podList.getItems())) {
        return null;
      }
      return podList.getItems().get(0);
    } catch (KubernetesClientException ex) {
      LOG.warn("Failed to resolve pod metadata from Kubernetes API", ex);
      return null;
    }
  }

  private String resolvePodName(ServiceInstance instance) {
    for (String key : properties.getPodNameMetadataKeys()) {
      String value = instance.getMetadata().get(key);
      if (StringUtils.hasText(value)) {
        return value.trim();
      }
    }
    String instanceId = instance.getInstanceId();
    if (StringUtils.hasText(instanceId)) {
      return instanceId;
    }
    return null;
  }

  private KubernetesPodMetadata extractMetadata(String namespace, Pod pod, ServiceInstance instance) {
    Map<String, String> annotations = safeMap(pod.getMetadata() != null ? pod.getMetadata().getAnnotations() : null);
    Map<String, String> labels = safeMap(pod.getMetadata() != null ? pod.getMetadata().getLabels() : null);

    String availability = resolveAvailability(pod, annotations);
    Double healthScore = resolveDouble(annotations, properties.getHealthScoreAnnotation(),
        "health-score", availabilityUp(availability) ? 1.0d : 0.0d);
    Double responseTime = resolveDouble(annotations, properties.getResponseTimeAnnotation(),
        "avg-response-time-ms", properties.getDefaultResponseTime().toMillis());
    String zone = resolveZone(labels, instance.getMetadata());
    String rollout = resolveRollout(annotations, labels);

    Map<String, String> additional = Map.of();
    return new KubernetesPodMetadata(namespace, availability, healthScore, responseTime, zone, rollout, additional);
  }

  private Map<String, String> safeMap(Map<String, String> map) {
    return map != null ? map : Map.of();
  }

  private String resolveAvailability(Pod pod, Map<String, String> annotations) {
    String override = annotations.get(properties.getAvailabilityAnnotation());
    if (StringUtils.hasText(override)) {
      return override.trim();
    }
    if (pod.getStatus() != null && pod.getStatus().getConditions() != null) {
      for (PodCondition condition : pod.getStatus().getConditions()) {
        if ("Ready".equalsIgnoreCase(condition.getType())) {
          return Boolean.parseBoolean(condition.getStatus()) ? "UP" : "DOWN";
        }
      }
    }
    return "UNKNOWN";
  }

  private boolean availabilityUp(String availability) {
    if (!StringUtils.hasText(availability)) {
      return false;
    }
    String normalized = availability.trim().toUpperCase(Locale.ROOT);
    return switch (normalized) {
      case "UP", "READY", "AVAILABLE" -> true;
      case "UNKNOWN" -> true;
      default -> false;
    };
  }

  private Double resolveDouble(Map<String, String> source, String primaryKey, String legacyKey, double fallback) {
    List<String> candidates = new ArrayList<>();
    if (StringUtils.hasText(primaryKey)) {
      candidates.add(primaryKey);
    }
    if (StringUtils.hasText(legacyKey)) {
      candidates.add(legacyKey);
    }
    for (String key : candidates) {
      if (!StringUtils.hasText(key)) {
        continue;
      }
      String value = source.get(key);
      if (!StringUtils.hasText(value)) {
        continue;
      }
      try {
        return Double.parseDouble(value.trim());
      } catch (NumberFormatException ignored) {
        // ignore and continue searching
      }
    }
    return fallback;
  }

  private String resolveZone(Map<String, String> labels, Map<String, String> instanceMetadata) {
    if (instanceMetadata != null) {
      for (String key : properties.getServiceZoneMetadataKeys()) {
        String value = instanceMetadata.get(key);
        if (StringUtils.hasText(value)) {
          return value.trim();
        }
      }
    }
    if (labels != null) {
      String direct = labels.get(properties.getZoneLabel());
      if (StringUtils.hasText(direct)) {
        return direct.trim();
      }
      for (String fallbackKey : properties.getZoneLabelFallbacks()) {
        String value = labels.get(fallbackKey);
        if (StringUtils.hasText(value)) {
          return value.trim();
        }
      }
    }
    return null;
  }

  private String resolveRollout(Map<String, String> annotations, Map<String, String> labels) {
    if (annotations != null && StringUtils.hasText(properties.getRolloutAnnotation())) {
      String value = annotations.get(properties.getRolloutAnnotation());
      if (StringUtils.hasText(value)) {
        return value.trim();
      }
    }
    if (labels != null && StringUtils.hasText(properties.getRolloutLabel())) {
      String value = labels.get(properties.getRolloutLabel());
      if (StringUtils.hasText(value)) {
        return value.trim();
      }
    }
    return null;
  }
}

