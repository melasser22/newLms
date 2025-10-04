package com.ejada.gateway.filter;

import com.ejada.common.constants.HeaderNames;
import com.ejada.gateway.config.GatewayRoutesProperties;
import com.ejada.gateway.context.GatewayRequestAttributes;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
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

  private static final Pattern VERSION_PATTERN = Pattern.compile("^v(\\d+)$", Pattern.CASE_INSENSITIVE);

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
    URI originalUri = request.getURI();
    String rawPath = originalUri.getRawPath();

    List<String> segments = tokenise(rawPath);
    boolean hadVersionSegment = false;
    String requestedVersion = null;

    if (!segments.isEmpty()) {
      String candidate = segments.get(0);
      if (isVersionSegment(candidate)) {
        hadVersionSegment = true;
        requestedVersion = normalise(candidate);
        segments.remove(0);
      }
    }

    String effectiveVersion = versioning.getDefaultVersion();
    if (hadVersionSegment && requestedVersion != null) {
      if (versioning.hasSupportedVersions() && !versioning.getSupportedVersions().contains(requestedVersion)) {
        if (!versioning.isFallbackToDefault()) {
          LOGGER.debug("Rejecting unsupported API version '{}' for route {}", requestedVersion, versioning);
          return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
              "Requested API version is not supported"));
        }
        LOGGER.debug("Falling back to default version {} for unsupported request version {}", effectiveVersion,
            requestedVersion);
      } else {
        effectiveVersion = requestedVersion;
      }
    }

    if (!hadVersionSegment && StringUtils.hasText(versioning.getDefaultVersion())) {
      effectiveVersion = versioning.getDefaultVersion();
    }

    String normalisedPath = rebuildPath(rawPath, segments);
    ServerHttpRequest.Builder requestBuilder = request.mutate();
    if (!normalisedPath.equals(rawPath)) {
      URI newUri = UriComponentsBuilder.fromUri(originalUri)
          .replacePath(normalisedPath)
          .build(true)
          .toUri();
      requestBuilder.uri(newUri);
    }

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

  private static boolean isVersionSegment(String value) {
    return StringUtils.hasText(value) && VERSION_PATTERN.matcher(value.trim()).matches();
  }

  private static String normalise(String value) {
    return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
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
}
