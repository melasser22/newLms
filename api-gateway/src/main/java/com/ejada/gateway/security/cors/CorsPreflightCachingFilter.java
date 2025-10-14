package com.ejada.gateway.security.cors;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Short-circuits CORS preflight requests by consulting an in-memory cache before
 * delegating to the rest of the filter chain.
 */
public class CorsPreflightCachingFilter implements WebFilter, Ordered {

  private final CorsConfigurationSource configurationSource;
  private final CorsPreflightCache cache;

  public CorsPreflightCachingFilter(CorsConfigurationSource configurationSource,
      CorsPreflightCache cache) {
    this.configurationSource = configurationSource;
    this.cache = cache;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    ServerHttpRequest request = exchange.getRequest();
    if (!isPreflightRequest(request)) {
      return chain.filter(exchange);
    }

    String key = buildCacheKey(request);
    return cache.lookup(key)
        .map(headers -> respondWithCached(exchange, headers))
        .orElseGet(() -> handleAndCache(exchange, chain, key));
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 10;
  }

  private Mono<Void> handleAndCache(ServerWebExchange exchange, WebFilterChain chain, String key) {
    CorsConfiguration configuration = configurationSource.getCorsConfiguration(exchange);
    if (configuration == null) {
      return chain.filter(exchange);
    }
    ServerHttpRequest request = exchange.getRequest();
    String origin = request.getHeaders().getOrigin();
    HttpMethod requestedMethod = request.getHeaders().getAccessControlRequestMethod();
    if (!StringUtils.hasText(origin) || requestedMethod == null) {
      return chain.filter(exchange);
    }

    String allowedOrigin = configuration.checkOrigin(origin);
    if (allowedOrigin == null) {
      return reject(exchange);
    }

    HttpMethod allowedMethod = configuration.checkHttpMethod(requestedMethod);
    if (allowedMethod == null) {
      return reject(exchange);
    }

    List<String> requestedHeaders = request.getHeaders().getAccessControlRequestHeaders();
    List<String> allowedHeaders = configuration.checkHeaders(requestedHeaders);
    if (CollectionUtils.isEmpty(allowedHeaders)) {
      allowedHeaders = requestedHeaders;
    }

    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.setAccessControlAllowOrigin(allowedOrigin);
    responseHeaders.setAccessControlAllowMethods(List.of(allowedMethod));
    if (!CollectionUtils.isEmpty(allowedHeaders)) {
      responseHeaders.setAccessControlAllowHeaders(allowedHeaders);
    }
    Boolean allowCredentials = configuration.getAllowCredentials();
    if (Boolean.TRUE.equals(allowCredentials)) {
      responseHeaders.setAccessControlAllowCredentials(true);
    }
    if (!CollectionUtils.isEmpty(configuration.getExposedHeaders())) {
      responseHeaders.setAccessControlExposeHeaders(configuration.getExposedHeaders());
    }
    long maxAge = Objects.requireNonNullElse(configuration.getMaxAge(), Duration.ofHours(1).getSeconds());
    responseHeaders.setAccessControlMaxAge(maxAge);
    responseHeaders.add(HttpHeaders.VARY, HttpHeaders.ORIGIN);
    responseHeaders.add(HttpHeaders.VARY, HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD);
    responseHeaders.add(HttpHeaders.VARY, HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);

    cache.store(key, responseHeaders);
    return respond(exchange, responseHeaders);
  }

  private Mono<Void> respondWithCached(ServerWebExchange exchange, HttpHeaders headers) {
    return respond(exchange, headers);
  }

  private Mono<Void> respond(ServerWebExchange exchange, HttpHeaders headers) {
    exchange.getResponse().setStatusCode(HttpStatus.NO_CONTENT);
    exchange.getResponse().getHeaders().putAll(headers);
    return exchange.getResponse().setComplete();
  }

  private Mono<Void> reject(ServerWebExchange exchange) {
    exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
    return exchange.getResponse().setComplete();
  }

  private boolean isPreflightRequest(ServerHttpRequest request) {
    return HttpMethod.OPTIONS.equals(request.getMethod())
        && request.getHeaders().getAccessControlRequestMethod() != null
        && StringUtils.hasText(request.getHeaders().getOrigin());
  }

  private String buildCacheKey(ServerHttpRequest request) {
    String origin = request.getHeaders().getOrigin();
    HttpMethod method = request.getHeaders().getAccessControlRequestMethod();
    List<String> headers = request.getHeaders().getAccessControlRequestHeaders();
    String headerKey = CollectionUtils.isEmpty(headers)
        ? ""
        : headers.stream()
            .filter(StringUtils::hasText)
            .map(value -> value.trim().toLowerCase(Locale.ROOT))
            .sorted(Comparator.naturalOrder())
            .collect(Collectors.joining(","));
    return origin + '|' + method + '|' + request.getURI().getPath() + '|' + headerKey;
  }
}
