package com.ejada.gateway.cache;

import com.ejada.common.constants.HeaderNames;
import com.ejada.gateway.config.GatewayCacheProperties.ClientProperties;
import com.ejada.gateway.config.GatewayCacheProperties.RouteCacheProperties;
import com.ejada.gateway.transformation.ResponseCacheService;
import com.ejada.gateway.transformation.ResponseCacheService.CacheMetadata;
import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

/**
 * Issues background refresh requests so stale cache entries are revalidated
 * without blocking live requests and performs scheduled warmups.
 */
@Component
@Lazy
public class CacheRefreshService {

  private static final Logger LOGGER = LoggerFactory.getLogger(CacheRefreshService.class);

  private static final String NO_QUERY_PLACEHOLDER = "-";

  private final ResponseCacheService cacheService;

  private final ServerProperties serverProperties;

  private final AtomicReference<WebClient> clientReference = new AtomicReference<>();

  private final String baseUrl;

  public CacheRefreshService(ResponseCacheService cacheService,
      ServerProperties serverProperties,
      ObjectProvider<WebClient.Builder> builderProvider) {
    this.cacheService = cacheService;
    this.serverProperties = serverProperties;
    this.baseUrl = localBaseUrl();
    WebClient.Builder builder = Optional.ofNullable(builderProvider.getIfAvailable())
        .orElseGet(WebClient::builder);
    this.clientReference.set(builder.baseUrl(baseUrl).build());
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
    UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl)
        .path(sanitizedPath);
    if (StringUtils.hasText(query) && !NO_QUERY_PLACEHOLDER.equals(query)) {
      builder.query(query);
    }
    return builder.build(true).toUri();
  }

  private String localBaseUrl() {
    Integer configuredPort = serverProperties.getPort();
    int port = (configuredPort == null || configuredPort == 0) ? 8000 : configuredPort;
    return "http://127.0.0.1:" + port;
  }
}
