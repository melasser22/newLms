package com.ejada.gateway.config;

import com.ejada.gateway.versioning.VersionNumber;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Global API versioning metadata that enables the gateway to normalise and migrate
 * legacy endpoints towards the newest service implementations.
 */
@RefreshScope
@ConfigurationProperties(prefix = "gateway.versioning")
public class GatewayVersioningProperties {

  private boolean enabled = true;

  @Valid
  private List<Mapping> mappings = new ArrayList<>();

  @Valid
  private Compatibility compatibility = new Compatibility();

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public List<Mapping> getMappings() {
    return mappings;
  }

  public void setMappings(List<Mapping> mappings) {
    this.mappings = (mappings == null) ? new ArrayList<>() : new ArrayList<>(mappings);
  }

  public Compatibility getCompatibility() {
    return compatibility;
  }

  public void setCompatibility(Compatibility compatibility) {
    this.compatibility = (compatibility == null) ? new Compatibility() : compatibility;
  }

  /**
   * Declarative compatibility matrix describing which API versions interoperate.
   */
  public static class Compatibility {

    private Map<String, List<String>> matrix = new LinkedHashMap<>();

    public Map<String, List<String>> getMatrix() {
      return matrix;
    }

    public void setMatrix(Map<String, List<String>> matrix) {
      this.matrix = (matrix == null) ? new LinkedHashMap<>() : normalise(matrix);
    }

    private Map<String, List<String>> normalise(Map<String, List<String>> input) {
      Map<String, List<String>> canonical = new LinkedHashMap<>();
      input.forEach((key, values) -> {
        if (!StringUtils.hasText(key)) {
          return;
        }
        String canonicalKey = VersionNumber.canonicaliseOrThrow(key.trim());
        List<String> canonicalValues = new ArrayList<>();
        if (values != null) {
          for (String value : values) {
            String canonicalValue = VersionNumber.canonicaliseOrNull(value);
            if (canonicalValue != null) {
              canonicalValues.add(canonicalValue);
            }
          }
        }
        canonical.put(canonicalKey, canonicalValues);
      });
      return canonical;
    }
  }

  /**
   * Mapping definition describing how legacy paths should be translated to the
   * canonical versioned endpoints.
   */
  public static class Mapping {

    @NotBlank
    private String id;

    private String description;

    private List<String> legacyPaths = new ArrayList<>();

    private List<String> methods = new ArrayList<>();

    private boolean fallbackToDefault = true;

    private String defaultVersion;

    @Valid
    private List<Route> routes = new ArrayList<>();

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public List<String> getLegacyPaths() {
      return legacyPaths;
    }

    public void setLegacyPaths(List<String> legacyPaths) {
      if (legacyPaths == null) {
        this.legacyPaths = new ArrayList<>();
        return;
      }
      List<String> processed = new ArrayList<>();
      for (String path : legacyPaths) {
        if (!StringUtils.hasText(path)) {
          continue;
        }
        String trimmed = path.trim();
        if (!trimmed.startsWith("/")) {
          trimmed = '/' + trimmed;
        }
        processed.add(trimmed);
      }
      this.legacyPaths = processed;
    }

    public List<String> getMethods() {
      return methods;
    }

    public void setMethods(List<String> methods) {
      if (methods == null) {
        this.methods = new ArrayList<>();
        return;
      }
      LinkedHashSet<String> unique = new LinkedHashSet<>();
      for (String method : methods) {
        if (!StringUtils.hasText(method)) {
          continue;
        }
        unique.add(method.trim().toUpperCase(Locale.ROOT));
      }
      this.methods = new ArrayList<>(unique);
    }

    public boolean isFallbackToDefault() {
      return fallbackToDefault;
    }

    public void setFallbackToDefault(boolean fallbackToDefault) {
      this.fallbackToDefault = fallbackToDefault;
    }

    public String getDefaultVersion() {
      return defaultVersion;
    }

    public void setDefaultVersion(String defaultVersion) {
      this.defaultVersion = VersionNumber.canonicaliseOrNull(defaultVersion);
    }

    public List<Route> getRoutes() {
      return routes;
    }

    public void setRoutes(List<Route> routes) {
      this.routes = (routes == null) ? new ArrayList<>() : new ArrayList<>(routes);
    }

    public void validate() {
      if (CollectionUtils.isEmpty(legacyPaths)) {
        throw new IllegalStateException("gateway.versioning.mappings." + id + ".legacy-paths must contain at least one entry");
      }
      if (CollectionUtils.isEmpty(routes)) {
        throw new IllegalStateException("gateway.versioning.mappings." + id + ".routes must contain at least one route definition");
      }
      LinkedHashMap<String, List<Route>> versions = new LinkedHashMap<>();
      for (Route route : routes) {
        route.validate(id);
        versions.computeIfAbsent(route.getVersion(), ignored -> new ArrayList<>()).add(route);
      }
      if (defaultVersion == null) {
        defaultVersion = routes.get(0).getTargetVersionOrSelf();
      }
      if (!versions.containsKey(defaultVersion) && fallbackToDefault) {
        throw new IllegalStateException("gateway.versioning.mappings." + id + " default version " + defaultVersion + " does not map to any route");
      }
    }
  }

  /**
   * Target route describing the migration rule for a particular API version.
   */
  public static class Route {

    @NotBlank
    private String version;

    private String targetVersion;

    private String rewritePath;

    private int weight = 100;

    private boolean deprecated;

    private String warning;

    private String sunset;

    private String policyLink;

    private List<String> compatibility = new ArrayList<>();

    private Map<String, String> additionalHeaders = new LinkedHashMap<>();

    private String documentationGroup;

    private Map<String, String> compatibilityTransformations = new LinkedHashMap<>();

    public String getVersion() {
      return version;
    }

    public void setVersion(String version) {
      this.version = VersionNumber.canonicaliseOrThrow(version);
    }

    public String getTargetVersion() {
      return targetVersion;
    }

    public void setTargetVersion(String targetVersion) {
      this.targetVersion = VersionNumber.canonicaliseOrNull(targetVersion);
    }

    public String getRewritePath() {
      return rewritePath;
    }

    public void setRewritePath(String rewritePath) {
      if (!StringUtils.hasText(rewritePath)) {
        this.rewritePath = null;
        return;
      }
      String value = rewritePath.trim();
      if (!value.startsWith("/")) {
        value = '/' + value;
      }
      this.rewritePath = value;
    }

    public int getWeight() {
      return weight;
    }

    public void setWeight(int weight) {
      this.weight = (weight <= 0) ? 1 : weight;
    }

    public boolean isDeprecated() {
      return deprecated;
    }

    public void setDeprecated(boolean deprecated) {
      this.deprecated = deprecated;
    }

    public String getWarning() {
      return warning;
    }

    public void setWarning(String warning) {
      this.warning = StringUtils.hasText(warning) ? warning.trim() : null;
    }

    public String getSunset() {
      return sunset;
    }

    public void setSunset(String sunset) {
      this.sunset = StringUtils.hasText(sunset) ? sunset.trim() : null;
    }

    public String getPolicyLink() {
      return policyLink;
    }

    public void setPolicyLink(String policyLink) {
      this.policyLink = StringUtils.hasText(policyLink) ? policyLink.trim() : null;
    }

    public List<String> getCompatibility() {
      return compatibility;
    }

    public void setCompatibility(List<String> compatibility) {
      if (compatibility == null) {
        this.compatibility = new ArrayList<>();
        return;
      }
      LinkedHashSet<String> canonical = new LinkedHashSet<>();
      for (String value : compatibility) {
        String canonicalVersion = VersionNumber.canonicaliseOrNull(value);
        if (canonicalVersion != null) {
          canonical.add(canonicalVersion);
        }
      }
      this.compatibility = new ArrayList<>(canonical);
    }

    public Map<String, String> getAdditionalHeaders() {
      return additionalHeaders;
    }

    public void setAdditionalHeaders(Map<String, String> additionalHeaders) {
      this.additionalHeaders = (additionalHeaders == null)
          ? new LinkedHashMap<>()
          : new LinkedHashMap<>(additionalHeaders);
    }

    public String getDocumentationGroup() {
      return documentationGroup;
    }

    public void setDocumentationGroup(String documentationGroup) {
      this.documentationGroup = StringUtils.hasText(documentationGroup) ? documentationGroup.trim() : null;
    }

    public Map<String, String> getCompatibilityTransformations() {
      return compatibilityTransformations;
    }

    public void setCompatibilityTransformations(Map<String, String> compatibilityTransformations) {
      LinkedHashMap<String, String> copy = new LinkedHashMap<>();
      if (compatibilityTransformations != null) {
        compatibilityTransformations.forEach((key, value) -> {
          String canonicalKey = VersionNumber.canonicaliseOrNull(key);
          if (canonicalKey == null || !StringUtils.hasText(value)) {
            return;
          }
          copy.put(canonicalKey, value.trim());
        });
      }
      this.compatibilityTransformations = copy;
    }

    void validate(String mappingId) {
      Objects.requireNonNull(version, "version");
      if (targetVersion != null && targetVersion.length() == 0) {
        targetVersion = null;
      }
      if (rewritePath == null) {
        throw new IllegalStateException("gateway.versioning.mappings." + mappingId + ".routes for version " + version + " must define a rewrite-path");
      }
    }

    public String getTargetVersionOrSelf() {
      return (targetVersion != null) ? targetVersion : version;
    }
  }
}
