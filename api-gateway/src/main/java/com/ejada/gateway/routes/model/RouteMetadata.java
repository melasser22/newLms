package com.ejada.gateway.routes.model;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.util.StringUtils;

/**
 * Arbitrary metadata associated with a dynamic route. Structured fields are exposed for
 * blue/green deployments and A/B testing while unknown attributes are preserved for UI
 * consumption.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RouteMetadata {

  private BlueGreenDeployment blueGreen;
  private final List<TrafficSplit> trafficSplits = new ArrayList<>();
  private final Map<String, Object> attributes = new LinkedHashMap<>();
  private Map<String, String> requestHeaders = new LinkedHashMap<>();
  private List<String> methods = new ArrayList<>();
  private Integer stripPrefix;
  private String prefixPath;

  public BlueGreenDeployment getBlueGreen() {
    return blueGreen;
  }

  public void setBlueGreen(BlueGreenDeployment blueGreen) {
    this.blueGreen = blueGreen;
  }

  public List<TrafficSplit> getTrafficSplits() {
    return trafficSplits;
  }

  public Map<String, Object> getAttributes() {
    return attributes;
  }

  public Map<String, String> getRequestHeaders() {
    return requestHeaders;
  }

  public void setRequestHeaders(Map<String, String> requestHeaders) {
    this.requestHeaders = (requestHeaders == null)
        ? new LinkedHashMap<>()
        : new LinkedHashMap<>(requestHeaders);
  }

  public List<String> getMethods() {
    return methods;
  }

  public void setMethods(List<String> methods) {
    this.methods = (methods == null) ? new ArrayList<>() : new ArrayList<>(methods);
  }

  public Integer getStripPrefix() {
    return stripPrefix;
  }

  public void setStripPrefix(Integer stripPrefix) {
    this.stripPrefix = stripPrefix;
  }

  public String getPrefixPath() {
    return prefixPath;
  }

  public void setPrefixPath(String prefixPath) {
    this.prefixPath = prefixPath;
  }

  @JsonAnySetter
  public void capture(String key, Object value) {
    if (key == null) {
      return;
    }
    attributes.put(key, value);
  }

  public Optional<URI> resolveEffectiveUri(URI defaultUri) {
    URI candidate = defaultUri;
    if (blueGreen != null) {
      candidate = blueGreen.resolveActiveUri().orElse(candidate);
    }
    return Optional.ofNullable(candidate);
  }

  public static RouteMetadata empty() {
    return new RouteMetadata();
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class BlueGreenDeployment {

    private String activeSlot;
    private URI blueUri;
    private URI greenUri;

    public Optional<String> getActiveSlot() {
      return Optional.ofNullable(activeSlot).map(String::trim).filter(StringUtils::hasText);
    }

    public void setActiveSlot(String activeSlot) {
      this.activeSlot = activeSlot;
    }

    public URI getBlueUri() {
      return blueUri;
    }

    public void setBlueUri(URI blueUri) {
      this.blueUri = blueUri;
    }

    public URI getGreenUri() {
      return greenUri;
    }

    public void setGreenUri(URI greenUri) {
      this.greenUri = greenUri;
    }

    public Optional<URI> resolveActiveUri() {
      String slot = getActiveSlot().map(String::toLowerCase).orElse("blue");
      if (Objects.equals(slot, "blue")) {
        return Optional.ofNullable(blueUri);
      }
      if (Objects.equals(slot, "green")) {
        return Optional.ofNullable(greenUri);
      }
      return Optional.empty();
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class TrafficSplit {

    private String variantId;
    private int percentage;
    private URI serviceUri;

    public String getVariantId() {
      return variantId;
    }

    public void setVariantId(String variantId) {
      this.variantId = variantId;
    }

    public int getPercentage() {
      return percentage;
    }

    public void setPercentage(int percentage) {
      this.percentage = percentage;
    }

    public URI getServiceUri() {
      return serviceUri;
    }

    public void setServiceUri(URI serviceUri) {
      this.serviceUri = serviceUri;
    }
  }
}
