package com.ejada.gateway.versioning;

import com.ejada.common.constants.HeaderNames;
import com.ejada.gateway.config.GatewayVersioningProperties;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.http.HttpMethod;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriTemplate;
import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPattern.PathMatchInfo;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * Resolves version normalisation rules declared via {@link GatewayVersioningProperties} for a
 * particular incoming request. The resolver is thread-safe after construction and caches compiled
 * path patterns for efficient lookups.
 */
public class VersionMappingResolver {

  private final boolean enabled;
  private final List<CompiledMapping> mappings;
  private final Map<String, List<String>> compatibilityMatrix;

  public VersionMappingResolver(GatewayVersioningProperties properties) {
    this.enabled = properties.isEnabled();
    this.compatibilityMatrix = normaliseCompatibility(properties.getCompatibility().getMatrix());
    this.mappings = compileMappings(properties.getMappings());
  }

  public boolean isEnabled() {
    return enabled && !mappings.isEmpty();
  }

  public Map<String, List<String>> getCompatibilityMatrix() {
    return compatibilityMatrix;
  }

  private Map<String, List<String>> normaliseCompatibility(Map<String, List<String>> matrix) {
    if (matrix == null || matrix.isEmpty()) {
      return Collections.emptyMap();
    }
    Map<String, List<String>> canonical = new LinkedHashMap<>();
    matrix.forEach((key, values) -> {
      if (!StringUtils.hasText(key)) {
        return;
      }
      String canonicalKey = VersionNumber.canonicaliseOrNull(key);
      if (canonicalKey == null) {
        return;
      }
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

  private List<CompiledMapping> compileMappings(List<GatewayVersioningProperties.Mapping> definitions) {
    if (definitions == null) {
      return List.of();
    }
    @SuppressWarnings("deprecation")
    PathPatternParser parser = new PathPatternParser();
    parser.setMatchOptionalTrailingSeparator(true);
    List<CompiledMapping> compiled = new ArrayList<>(definitions.size());
    for (GatewayVersioningProperties.Mapping mapping : definitions) {
      mapping.validate();
      compiled.add(new CompiledMapping(mapping, parser));
    }
    return compiled;
  }

  /**
   * Attempts to resolve a version mapping for the current request.
   */
  public Optional<VersionMappingResult> resolve(ServerWebExchange exchange) {
    if (!isEnabled()) {
      return Optional.empty();
    }
    String method = Optional.ofNullable(exchange.getRequest().getMethod())
        .map(HttpMethod::name)
        .orElse(null);

    List<String> candidatePaths = new ArrayList<>(2);
    URI requestUri = exchange.getRequest().getURI();
    candidatePaths.add(requestUri.getRawPath());
    URI gatewayRequestUrl = exchange.getAttribute(org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
    if (gatewayRequestUrl != null && !Objects.equals(gatewayRequestUrl.getRawPath(), requestUri.getRawPath())) {
      candidatePaths.add(gatewayRequestUrl.getRawPath());
    }

    String headerVersion = VersionNumber.canonicaliseOrNull(exchange.getRequest().getHeaders().getFirst(HeaderNames.ACCEPT_VERSION));

    for (CompiledMapping mapping : mappings) {
      if (!mapping.matchesMethod(method)) {
        continue;
      }
      for (String path : candidatePaths) {
        VersionMappingResult result = mapping.resolve(path, headerVersion);
        if (result == null) {
          continue;
        }
        if (result.isUnsupported()) {
          return Optional.of(result);
        }
        // Merge compatibility metadata from global matrix if available.
        if (!compatibilityMatrix.isEmpty() && StringUtils.hasText(result.getResolvedVersion())) {
          List<String> additional = compatibilityMatrix.get(result.getResolvedVersion());
          if (!CollectionUtils.isEmpty(additional)) {
            List<String> merged = new ArrayList<>(result.getCompatibility());
            for (String value : additional) {
              if (!merged.contains(value)) {
                merged.add(value);
              }
            }
            result = result.withCompatibility(merged);
          }
        }
        return Optional.of(result);
      }
    }
    return Optional.empty();
  }

  private static final class CompiledMapping {

    private final GatewayVersioningProperties.Mapping configuration;
    private final List<PatternHolder> patterns;
    private final Map<String, List<GatewayVersioningProperties.Route>> routesByVersion;

    private CompiledMapping(GatewayVersioningProperties.Mapping configuration, PathPatternParser parser) {
      this.configuration = configuration;
      this.patterns = compilePatterns(configuration.getLegacyPaths(), parser);
      this.routesByVersion = indexRoutes(configuration.getRoutes());
    }

    private boolean matchesMethod(String method) {
      if (configuration.getMethods().isEmpty() || !StringUtils.hasText(method)) {
        return true;
      }
      return configuration.getMethods().contains(method.toUpperCase(Locale.ROOT));
    }

    private VersionMappingResult resolve(String path, String headerVersion) {
      for (PatternHolder holder : patterns) {
        PathMatchInfo info = holder.pattern.matchAndExtract(PathContainer.parsePath(path));
        if (info == null) {
          continue;
        }
        return doResolve(path, headerVersion, info, holder);
      }
      return null;
    }

    private VersionMappingResult doResolve(String path, String headerVersion, PathMatchInfo info, PatternHolder holder) {
      String requestedVersion = determineRequestedVersion(headerVersion, info, path);
      String effectiveVersion = requestedVersion;
      List<GatewayVersioningProperties.Route> candidates = routesByVersion.getOrDefault(effectiveVersion, List.of());

      if (candidates.isEmpty() && configuration.isFallbackToDefault() && configuration.getDefaultVersion() != null) {
        effectiveVersion = configuration.getDefaultVersion();
        candidates = routesByVersion.getOrDefault(effectiveVersion, List.of());
      }

      if (candidates.isEmpty()) {
        if (StringUtils.hasText(requestedVersion)) {
          return VersionMappingResult.unsupported(configuration.getId(), requestedVersion);
        }
        return null;
      }

      GatewayVersioningProperties.Route selected = selectWeighted(candidates);
      String resolvedVersion = selected.getTargetVersionOrSelf();
      String rewritten = rewritePath(path, info, selected.getRewritePath());

      return VersionMappingResult.resolved(configuration.getId(), requestedVersion, resolvedVersion, rewritten,
          selected.isDeprecated(), selected.getWarning(), selected.getSunset(), selected.getPolicyLink(),
          List.copyOf(selected.getCompatibility()), new LinkedHashMap<>(selected.getAdditionalHeaders()),
          selected.getDocumentationGroup(), headerVersion != null ? "header" : holder.versionSource);
    }

    private String determineRequestedVersion(String headerVersion, PathMatchInfo info, String path) {
      if (headerVersion != null) {
        return headerVersion;
      }
      Map<String, String> variables = info.getUriVariables();
      if (variables.containsKey("version")) {
        String canonical = VersionNumber.canonicaliseOrNull(variables.get("version"));
        if (canonical != null) {
          return canonical;
        }
      }
      // Inspect path segments sequentially to locate the first valid version token.
      String[] segments = path.split("/");
      for (String segment : segments) {
        if (!StringUtils.hasText(segment)) {
          continue;
        }
        String canonical = VersionNumber.canonicaliseOrNull(segment);
        if (canonical != null) {
          return canonical;
        }
      }
      return configuration.getDefaultVersion();
    }

    private GatewayVersioningProperties.Route selectWeighted(List<GatewayVersioningProperties.Route> candidates) {
      if (candidates.size() == 1) {
        return candidates.get(0);
      }
      int total = candidates.stream().mapToInt(GatewayVersioningProperties.Route::getWeight).sum();
      if (total <= 0) {
        return candidates.get(0);
      }
      int pick = ThreadLocalRandom.current().nextInt(total);
      int cumulative = 0;
      for (GatewayVersioningProperties.Route candidate : candidates) {
        cumulative += Math.max(candidate.getWeight(), 0);
        if (pick < cumulative) {
          return candidate;
        }
      }
      return candidates.get(candidates.size() - 1);
    }

    private String rewritePath(String originalPath, PathMatchInfo info, String template) {
      if (!StringUtils.hasText(template)) {
        return originalPath;
      }
      Map<String, String> variables = new LinkedHashMap<>(info.getUriVariables());
      UriTemplate uriTemplate = new UriTemplate(template);
      for (String variableName : uriTemplate.getVariableNames()) {
        variables.putIfAbsent(variableName, "");
      }
      return uriTemplate.expand(variables).getPath();
    }

    private Map<String, List<GatewayVersioningProperties.Route>> indexRoutes(List<GatewayVersioningProperties.Route> routes) {
      Map<String, List<GatewayVersioningProperties.Route>> index = new LinkedHashMap<>();
      for (GatewayVersioningProperties.Route route : routes) {
        index.computeIfAbsent(route.getVersion(), ignored -> new ArrayList<>()).add(route);
      }
      return index;
    }

    private List<PatternHolder> compilePatterns(List<String> legacyPaths, PathPatternParser parser) {
      List<PatternHolder> holders = new ArrayList<>(legacyPaths.size());
      for (String path : legacyPaths) {
        PathPattern pattern = parser.parse(path);
        holders.add(new PatternHolder(pattern, inferVersionSource(path)));
      }
      return holders;
    }

    private String inferVersionSource(String path) {
      if (path.contains("{version}")) {
        return "path-template";
      }
      if (path.matches(".*/v\\d+.*")) {
        return "path";
      }
      return "default";
    }
  }

  private record PatternHolder(PathPattern pattern, String versionSource) {
  }
}
