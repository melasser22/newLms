package com.ejada.gateway.versioning;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Result returned by {@link VersionMappingResolver} describing the resolved target version and any
 * associated metadata (headers, deprecation warnings, documentation group, etc.).
 */
public final class VersionMappingResult {

  private final String mappingId;
  private final String requestedVersion;
  private final String resolvedVersion;
  private final String rewrittenPath;
  private final boolean unsupported;
  private final boolean deprecated;
  private final String warning;
  private final String sunset;
  private final String policyLink;
  private final List<String> compatibility;
  private final Map<String, String> additionalHeaders;
  private final String documentationGroup;
  private final String resolutionSource;
  private final Map<String, String> compatibilityTransformations;

  private VersionMappingResult(String mappingId,
      String requestedVersion,
      String resolvedVersion,
      String rewrittenPath,
      boolean unsupported,
      boolean deprecated,
      String warning,
      String sunset,
      String policyLink,
      List<String> compatibility,
      Map<String, String> additionalHeaders,
      String documentationGroup,
      String resolutionSource,
      Map<String, String> compatibilityTransformations) {
    this.mappingId = mappingId;
    this.requestedVersion = requestedVersion;
    this.resolvedVersion = resolvedVersion;
    this.rewrittenPath = rewrittenPath;
    this.unsupported = unsupported;
    this.deprecated = deprecated;
    this.warning = warning;
    this.sunset = sunset;
    this.policyLink = policyLink;
    this.compatibility = compatibility == null ? List.of() : List.copyOf(compatibility);
    this.additionalHeaders = additionalHeaders == null ? Map.of() : Map.copyOf(additionalHeaders);
    this.documentationGroup = documentationGroup;
    this.resolutionSource = resolutionSource;
    this.compatibilityTransformations = compatibilityTransformations == null
        ? Map.of()
        : Map.copyOf(compatibilityTransformations);
  }

  public static VersionMappingResult resolved(String mappingId,
      String requestedVersion,
      String resolvedVersion,
      String rewrittenPath,
      boolean deprecated,
      String warning,
      String sunset,
      String policyLink,
      List<String> compatibility,
      Map<String, String> additionalHeaders,
      String documentationGroup,
      String resolutionSource,
      Map<String, String> compatibilityTransformations) {
    return new VersionMappingResult(mappingId,
        requestedVersion,
        resolvedVersion,
        rewrittenPath,
        false,
        deprecated,
        warning,
        sunset,
        policyLink,
        compatibility,
        additionalHeaders,
        documentationGroup,
        resolutionSource,
        compatibilityTransformations);
  }

  public static VersionMappingResult unsupported(String mappingId, String requestedVersion) {
    return new VersionMappingResult(mappingId,
        requestedVersion,
        null,
        null,
        true,
        false,
        null,
        null,
        null,
        List.of(),
        Map.of(),
        null,
        "unsupported",
        Map.of());
  }

  public VersionMappingResult withCompatibility(List<String> compatibility) {
    return new VersionMappingResult(mappingId,
        requestedVersion,
        resolvedVersion,
        rewrittenPath,
        unsupported,
        deprecated,
        warning,
        sunset,
        policyLink,
        compatibility,
        additionalHeaders,
        documentationGroup,
        resolutionSource,
        compatibilityTransformations);
  }

  public boolean isUnsupported() {
    return unsupported;
  }

  public boolean isDeprecated() {
    return deprecated;
  }

  public String getRequestedVersion() {
    return requestedVersion;
  }

  public String getResolvedVersion() {
    return resolvedVersion;
  }

  public String getRewrittenPath() {
    return rewrittenPath;
  }

  public String getWarning() {
    return warning;
  }

  public String getSunset() {
    return sunset;
  }

  public String getPolicyLink() {
    return policyLink;
  }

  public List<String> getCompatibility() {
    return compatibility;
  }

  public Map<String, String> getAdditionalHeaders() {
    return additionalHeaders;
  }

  public String getDocumentationGroup() {
    return documentationGroup;
  }

  public String getResolutionSource() {
    return resolutionSource;
  }

  public String getMappingId() {
    return mappingId;
  }

  public Map<String, String> getCompatibilityTransformations() {
    return compatibilityTransformations;
  }

  public VersionMappingResult mergeHeaders(Map<String, String> globalHeaders) {
    if (globalHeaders == null || globalHeaders.isEmpty()) {
      return this;
    }
    Map<String, String> merged = new LinkedHashMap<>(additionalHeaders);
    merged.putAll(globalHeaders);
    return new VersionMappingResult(mappingId,
        requestedVersion,
        resolvedVersion,
        rewrittenPath,
        unsupported,
        deprecated,
        warning,
        sunset,
        policyLink,
        compatibility,
        merged,
        documentationGroup,
        resolutionSource,
        compatibilityTransformations);
  }

  @Override
  public String toString() {
    return "VersionMappingResult{"
        + "mappingId='" + mappingId + '\''
        + ", requestedVersion='" + requestedVersion + '\''
        + ", resolvedVersion='" + resolvedVersion + '\''
        + ", rewrittenPath='" + rewrittenPath + '\''
        + ", deprecated=" + deprecated
        + ", warning='" + warning + '\''
        + '}';
  }
}
