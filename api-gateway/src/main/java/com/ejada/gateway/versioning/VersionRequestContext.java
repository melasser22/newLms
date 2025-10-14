package com.ejada.gateway.versioning;

import com.ejada.common.constants.HeaderNames;
import com.ejada.gateway.context.GatewayRequestAttributes;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.server.PathContainer;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern.PathMatchInfo;

final class VersionRequestContext {

  private static final Pattern VENDOR_SUFFIX_PATTERN = Pattern.compile(
      "vnd\\.[^;+/]+\\.v(?<version>\\d+(?:\\.\\d+)*)");

  private static final Pattern VERSION_PARAMETER_PATTERN = Pattern.compile(
      "version=(?<version>\\d+(?:\\.\\d+)*)");

  private final ServerWebExchange exchange;
  private final String headerVersion;
  private final String acceptVersion;
  private final String vendorMediaTypeVersion;
  private final String queryVersion;
  private final String preferredVersion;

  private VersionRequestContext(ServerWebExchange exchange,
      String headerVersion,
      String acceptVersion,
      String vendorMediaTypeVersion,
      String queryVersion,
      String preferredVersion) {
    this.exchange = exchange;
    this.headerVersion = headerVersion;
    this.acceptVersion = acceptVersion;
    this.vendorMediaTypeVersion = vendorMediaTypeVersion;
    this.queryVersion = queryVersion;
    this.preferredVersion = preferredVersion;
  }

  static VersionRequestContext from(ServerWebExchange exchange) {
    String headerVersion = VersionNumber.canonicaliseOrNull(
        exchange.getRequest().getHeaders().getFirst(HeaderNames.API_VERSION));
    String acceptVersion = VersionNumber.canonicaliseOrNull(
        exchange.getRequest().getHeaders().getFirst(HeaderNames.ACCEPT_VERSION));
    String vendorMediaTypeVersion = extractVendorMediaTypeVersion(
        exchange.getRequest().getHeaders().getOrEmpty(HeaderNames.ACCEPT));
    String queryVersion = extractQueryVersion(exchange.getRequest().getQueryParams());
    String preferred = VersionNumber.canonicaliseOrNull(
        exchange.getAttribute(GatewayRequestAttributes.API_VERSION_PREFERENCE));

    return new VersionRequestContext(exchange, headerVersion, acceptVersion, vendorMediaTypeVersion,
        queryVersion, preferred);
  }

  VersionCandidate determine(PathMatchInfo info, String path, String defaultVersion) {
    if (headerVersion != null) {
      return new VersionCandidate(headerVersion, "header:x-api-version", true);
    }
    if (acceptVersion != null) {
      return new VersionCandidate(acceptVersion, "header:accept-version", true);
    }
    if (vendorMediaTypeVersion != null) {
      return new VersionCandidate(vendorMediaTypeVersion, "header:accept", true);
    }
    if (queryVersion != null) {
      return new VersionCandidate(queryVersion, "query", true);
    }
    if (info != null) {
      String fromTemplate = info.getUriVariables().get("version");
      String canonical = VersionNumber.canonicaliseOrNull(fromTemplate);
      if (canonical != null) {
        return new VersionCandidate(canonical, "path-template", true);
      }
    }
    String fromPath = extractFromPath(path);
    if (fromPath != null) {
      return new VersionCandidate(fromPath, "path", true);
    }
    if (preferredVersion != null) {
      return new VersionCandidate(preferredVersion, "preference", false);
    }
    if (defaultVersion != null) {
      return new VersionCandidate(defaultVersion, "default", false);
    }
    return new VersionCandidate(null, "unknown", false);
  }

  ServerWebExchange exchange() {
    return exchange;
  }

  private static String extractQueryVersion(MultiValueMap<String, String> queryParams) {
    if (queryParams == null) {
      return null;
    }
    String candidate = firstNonBlank(queryParams.getFirst("version"),
        queryParams.getFirst("api-version"),
        queryParams.getFirst("apiVersion"));
    return VersionNumber.canonicaliseOrNull(candidate);
  }

  private static String extractFromPath(String path) {
    if (!StringUtils.hasText(path)) {
      return null;
    }
    PathContainer container = PathContainer.parsePath(path);
    for (PathContainer.Element element : container.elements()) {
      if (element instanceof PathContainer.PathSegment segment) {
        String canonical = VersionNumber.canonicaliseOrNull(segment.value());
        if (canonical != null) {
          return canonical;
        }
      }
    }
    return null;
  }

  private static String extractVendorMediaTypeVersion(List<String> acceptValues) {
    if (acceptValues == null) {
      return null;
    }
    for (String raw : acceptValues) {
      if (!StringUtils.hasText(raw)) {
        continue;
      }
      String lower = raw.toLowerCase(Locale.ROOT);
      Matcher parameter = VERSION_PARAMETER_PATTERN.matcher(lower);
      if (parameter.find()) {
        String canonical = VersionNumber.canonicaliseOrNull(parameter.group("version"));
        if (canonical != null) {
          return canonical;
        }
      }
      Matcher matcher = VENDOR_SUFFIX_PATTERN.matcher(lower);
      if (matcher.find()) {
        String canonical = VersionNumber.canonicaliseOrNull(matcher.group("version"));
        if (canonical != null) {
          return canonical;
        }
      }
    }
    return null;
  }

  private static String firstNonBlank(String... values) {
    if (values == null) {
      return null;
    }
    for (String value : values) {
      if (StringUtils.hasText(value)) {
        return value;
      }
    }
    return null;
  }

  record VersionCandidate(String version, String source, boolean explicit) {
  }
}
