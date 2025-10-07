package com.ejada.gateway.cache;

import com.ejada.common.constants.HeaderNames;
import com.ejada.gateway.config.GatewayCacheProperties.ClientProperties;
import com.ejada.gateway.config.GatewayCacheProperties.RouteCacheProperties;
import com.ejada.gateway.transformation.ResponseCacheService;
import com.ejada.gateway.transformation.ResponseCacheService.CacheMetadata;
import java.net.URI;
import java.net.InetAddress;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

/**
 * Issues background refresh requests so stale cache entries are revalidated
 * without blocking live requests and performs scheduled warmups.
 */
@Component
public class CacheRefreshService {

  private static final Logger LOGGER = LoggerFactory.getLogger(CacheRefreshService.class);

  private static final String NO_QUERY_PLACEHOLDER = "-";

  private final ResponseCacheService cacheService;

  private final ServerProperties serverProperties;

  private final AtomicReference<WebClient> clientReference = new AtomicReference<>();

  private final AtomicReference<java.net.URI> baseUriReference = new AtomicReference<>();

  private final ObjectProvider<WebClient.Builder> builderProvider;

  public CacheRefreshService(ResponseCacheService cacheService,
      ServerProperties serverProperties,
      ObjectProvider<WebClient.Builder> builderProvider) {
    this.cacheService = cacheService;
    this.serverProperties = serverProperties;
    this.builderProvider = builderProvider;
    java.net.URI initialBase = resolveLocalBaseUri(null);
    this.baseUriReference.set(initialBase);
    this.clientReference.set(newClient(initialBase));
  }

  public void scheduleRevalidation(CacheMetadata metadata) {
    if (metadata == null || !cacheService.isCacheEnabled()) {
      return;
    }
    if (cacheService.isRefreshInFlight(metadata)) {
      return;
    }
    cacheService.trackRefreshStart(metadata);
    RouteCacheProperties route = metadata.route();
    boolean tenantScoped = route != null && route.isTenantScoped();
    dispatch(route, metadata.method(), metadata.canonicalPath(), metadata.canonicalQuery(), metadata.tenantId(),
        tenantScoped, true)
        .doFinally(signal -> cacheService.trackRefreshComplete(metadata))
        .subscribe();
  }

  public Mono<Void> warmRoute(RouteCacheProperties route, String candidatePath, String tenantId) {
    if (route == null || !cacheService.isCacheEnabled() || !StringUtils.hasText(candidatePath)) {
      return Mono.empty();
    }
    HttpMethod method = route.getMethod() != null ? route.getMethod() : HttpMethod.GET;
    String path = candidatePath;
    String query = NO_QUERY_PLACEHOLDER;
    int queryIndex = candidatePath.indexOf('?');
    if (queryIndex >= 0) {
      path = candidatePath.substring(0, queryIndex);
      query = candidatePath.substring(queryIndex + 1);
    }
    return dispatch(route, method, path, query, tenantId, route.isTenantScoped(), true);
  }

  private Mono<Void> dispatch(RouteCacheProperties route,
      HttpMethod method,
      String path,
      String query,
      String tenantId,
      boolean tenantScoped,
      boolean forceRefresh) {
    WebClient client = clientReference.get();
    if (client == null) {
      return Mono.empty();
    }
    HttpMethod verb = method != null ? method : HttpMethod.GET;
    URI requestUri = resolveRequestUri(path, query);
    ClientProperties clientProperties = route != null ? route.getClient() : null;
    Duration timeout = (clientProperties != null) ? clientProperties.resolvedTimeout() : Duration.ofSeconds(10);
    return client.method(verb)
        .uri(requestUri)
        .headers(headers -> {
          if (tenantScoped && StringUtils.hasText(tenantId)) {
            headers.add(HeaderNames.X_TENANT_ID, tenantId);
          }
          if (forceRefresh) {
            headers.add(HttpHeaders.CACHE_CONTROL, "max-age=0, no-cache");
          }
          headers.add(HttpHeaders.ACCEPT, "application/json");
          if (clientProperties != null) {
            if (StringUtils.hasText(clientProperties.getAuthorization())) {
              headers.set(HttpHeaders.AUTHORIZATION, clientProperties.getAuthorization());
            }
            if (StringUtils.hasText(clientProperties.getApiKey())) {
              headers.set(HeaderNames.API_KEY, clientProperties.getApiKey());
            }
            clientProperties.getHeaders().forEach(headers::add);
          }
        })
        .retrieve()
        .toBodilessEntity()
        .timeout(timeout)
        .doOnError(ex -> LOGGER.debug("Background cache refresh failed for {} {}", verb, path, ex))
        .onErrorResume(ex -> Mono.empty())
        .then();
  }

  private URI resolveRequestUri(String path, String query) {
    String sanitizedPath = StringUtils.hasText(path) ? path : "/";
    if (!sanitizedPath.startsWith("/")) {
      sanitizedPath = "/" + sanitizedPath;
    }
    java.net.URI baseUri = baseUriReference.get();
    Assert.notNull(baseUri, "Base URI must be available");
    UriComponentsBuilder builder = UriComponentsBuilder.fromUri(baseUri)
        .path(sanitizedPath);
    if (StringUtils.hasText(query) && !NO_QUERY_PLACEHOLDER.equals(query)) {
      builder.query(query);
    }
    return builder.build(true).toUri();
  }

  @EventListener(WebServerInitializedEvent.class)
  public void onWebServerInitialized(WebServerInitializedEvent event) {
    int actualPort = (event != null && event.getWebServer() != null) ? event.getWebServer().getPort() : -1;
    java.net.URI updated = resolveLocalBaseUri(actualPort > 0 ? actualPort : null);
    updateBaseUri(updated);
  }

  private void updateBaseUri(java.net.URI newBaseUri) {
    if (newBaseUri == null) {
      return;
    }
    java.net.URI current = baseUriReference.get();
    if (newBaseUri.equals(current)) {
      return;
    }
    baseUriReference.set(newBaseUri);
    clientReference.set(newClient(newBaseUri));
  }

  private WebClient newClient(java.net.URI baseUri) {
    WebClient.Builder builder = Optional.ofNullable(builderProvider.getIfAvailable())
        .orElseGet(WebClient::builder);
    return builder.baseUrl(baseUri.toString()).build();
  }

  private java.net.URI resolveLocalBaseUri(Integer overridePort) {
    String scheme = (serverProperties.getSsl() != null && serverProperties.getSsl().isEnabled())
        ? "https"
        : "http";
    InetAddress address = serverProperties.getAddress();
    String host = (address != null) ? address.getHostAddress() : "127.0.0.1";
    if (!StringUtils.hasText(host) || "0.0.0.0".equals(host) || "::".equals(host)) {
      host = "127.0.0.1";
    }
    Integer configuredPort = overridePort != null ? overridePort : serverProperties.getPort();
    int port = (configuredPort == null || configuredPort == 0) ? 8000 : configuredPort;
    String contextPath = serverProperties.getServlet() != null ? serverProperties.getServlet().getContextPath() : null;
    String sanitizedContextPath = sanitizeContextPath(contextPath);
    return UriComponentsBuilder.newInstance()
        .scheme(scheme)
        .host(host)
        .port(port)
        .path(sanitizedContextPath)
        .build()
        .toUri();
  }

  private String sanitizeContextPath(String contextPath) {
    if (!StringUtils.hasText(contextPath)) {
      return "";
    }
    String trimmed = contextPath.trim();
    if (!trimmed.startsWith("/")) {
      trimmed = "/" + trimmed;
    }
    if (trimmed.endsWith("/")) {
      trimmed = trimmed.substring(0, trimmed.length() - 1);
    }
    return trimmed;
  }
}
