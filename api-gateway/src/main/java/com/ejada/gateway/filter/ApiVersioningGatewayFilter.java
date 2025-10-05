package com.ejada.gateway.filter;

import com.ejada.common.constants.HeaderNames;
import com.ejada.gateway.config.GatewayRoutesProperties;
import com.ejada.gateway.context.GatewayRequestAttributes;
import com.ejada.gateway.versioning.VersionNumber;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

/**
 * Normalises versioned API requests so downstream services do not have to parse
 * URI prefixes such as {@code /v1/...}. The filter optionally strips the
 * version segment from the request path, falls back to a default version when a
 * request omits the version, and propagates the resolved version through a
 * canonical header.
 */
public class ApiVersioningGatewayFilter implements GatewayFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(ApiVersioningGatewayFilter.class);

  private final GatewayRoutesProperties.ServiceRoute.Versioning versioning;

  public ApiVersioningGatewayFilter(GatewayRoutesProperties.ServiceRoute.Versioning versioning) {
    this.versioning = Objects.requireNonNull(versioning, "versioning");
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    if (!versioning.isEnabled()) {
      return chain.filter(exchange);
    }

    ServerHttpRequest request = exchange.getRequest();
    String presetVersion = exchange.getAttribute(GatewayRequestAttributes.API_VERSION);
    if (StringUtils.hasText(presetVersion)) {
      if (versioning.isPropagateHeader()) {
        String currentHeader = request.getHeaders().getFirst(HeaderNames.API_VERSION);
        if (!presetVersion.equalsIgnoreCase(currentHeader)) {
          ServerHttpRequest mutated = request.mutate()
              .headers(headers -> headers.set(HeaderNames.API_VERSION, presetVersion))
              .build();
          return chain.filter(exchange.mutate().request(mutated).build());
        }
      }
      return chain.filter(exchange);
    }

    URI originalUri = request.getURI();
    String rawPath = originalUri.getRawPath();

    List<String> segments = tokenise(rawPath);
    VersionResolution resolution = resolveVersion(segments);
    if (resolution.isUnsupported()) {
      return rejectInvalidVersion(exchange, resolution.getResolvedVersion());
    }

    String effectiveVersion = resolution.getResolvedVersion();
    if (!resolution.hadExplicitVersion() && StringUtils.hasText(versioning.getDefaultVersion())) {
      effectiveVersion = versioning.getDefaultVersion();
    }

    String normalisedPath = rebuildPath(rawPath, segments);
    if (!normalisedPath.equals(rawPath)) {
      URI newUri = UriComponentsBuilder.fromUri(originalUri)
          .replacePath(normalisedPath)
          .build(true)
          .toUri();
      ServerWebExchangeUtils.addOriginalRequestUrl(exchange, originalUri);
      ServerWebExchangeUtils.resetRequestUri(exchange, newUri);
      request = exchange.getRequest();
    }

    ServerHttpRequest.Builder requestBuilder = request.mutate();

    if (versioning.isPropagateHeader()) {
      final String headerValue = effectiveVersion;
      requestBuilder.headers(httpHeaders -> httpHeaders.set(HeaderNames.API_VERSION, headerValue));
    }

    ServerHttpRequest mutatedRequest = requestBuilder.build();
    ServerWebExchange mutatedExchange = exchange.mutate()
        .request(mutatedRequest)
        .build();
    mutatedExchange.getAttributes().put(GatewayRequestAttributes.API_VERSION, effectiveVersion);
    return chain.filter(mutatedExchange);
  }

  private static List<String> tokenise(String path) {
    if (!StringUtils.hasText(path)) {
      return new ArrayList<>();
    }
    String[] rawSegments = path.split("/");
    List<String> result = new ArrayList<>(rawSegments.length);
    for (String segment : rawSegments) {
      if (!StringUtils.hasText(segment)) {
        continue;
      }
      result.add(segment);
    }
    return result;
  }

  private static String rebuildPath(String originalPath, List<String> segments) {
    if (segments.isEmpty()) {
      return originalPath.startsWith("/") ? "/" : "";
    }
    String joined = String.join("/", segments);
    String rebuilt = '/' + joined;
    if (originalPath.endsWith("/") && !rebuilt.endsWith("/")) {
      rebuilt = rebuilt + '/';
    }
    return rebuilt;
  }

  private VersionResolution resolveVersion(List<String> pathSegments) {
    boolean hadExplicitVersion = false;
    String effectiveVersion = versioning.getDefaultVersion();

    if (!pathSegments.isEmpty()) {
      String candidate = pathSegments.get(0);
      String canonical = canonicalise(candidate);
      if (canonical != null) {
        hadExplicitVersion = true;
        pathSegments.remove(0);

        if (versioning.hasSupportedVersions() && !versioning.getSupportedVersions().contains(canonical)) {
          LOGGER.debug("Rejecting unsupported API version '{}' for route {}", canonical, versioning);
          return VersionResolution.unsupportedVersion(canonical);
        }
        effectiveVersion = canonical;
      }
    }

    return VersionResolution.resolved(hadExplicitVersion, effectiveVersion);
  }

  private static String canonicalise(String candidate) {
    String canonical = VersionNumber.canonicaliseOrNull(candidate);
    if (canonical == null) {
      LOGGER.debug("Ignoring non-version path segment '{}'", candidate);
    }
    return canonical;
  }

  private static Mono<Void> rejectInvalidVersion(ServerWebExchange exchange, String version) {
    if (exchange != null) {
      exchange.getAttributes().put(GatewayRequestAttributes.API_VERSION, version);
    }
    return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Requested API version is not supported"));
  }

  private static final class VersionResolution {

    private final boolean hadExplicitVersion;
    private final String resolvedVersion;
    private final boolean unsupported;

    private VersionResolution(boolean hadExplicitVersion, String resolvedVersion, boolean unsupported) {
      this.hadExplicitVersion = hadExplicitVersion;
      this.resolvedVersion = resolvedVersion;
      this.unsupported = unsupported;
    }

    static VersionResolution resolved(boolean hadExplicitVersion, String resolvedVersion) {
      return new VersionResolution(hadExplicitVersion, resolvedVersion, false);
    }

    static VersionResolution unsupportedVersion(String version) {
      return new VersionResolution(true, version, true);
    }

    boolean hadExplicitVersion() {
      return hadExplicitVersion;
    }

    String getResolvedVersion() {
      return resolvedVersion;
    }

    boolean isUnsupported() {
      return unsupported;
    }
  }
}
