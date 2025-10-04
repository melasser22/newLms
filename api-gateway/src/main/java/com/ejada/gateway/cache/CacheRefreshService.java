package com.ejada.gateway.cache;

import com.ejada.common.constants.HeaderNames;
import com.ejada.gateway.config.GatewayCacheProperties.RouteCacheProperties;
import com.ejada.gateway.transformation.ResponseCacheService;
import com.ejada.gateway.transformation.ResponseCacheService.CacheMetadata;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.web.ServerProperties;
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
public class CacheRefreshService {

  private static final Logger LOGGER = LoggerFactory.getLogger(CacheRefreshService.class);

  private final ResponseCacheService cacheService;

  private final ServerProperties serverProperties;

  private final AtomicReference<WebClient> clientReference = new AtomicReference<>();

  public CacheRefreshService(ResponseCacheService cacheService,
      ServerProperties serverProperties,
      ObjectProvider<WebClient.Builder> builderProvider) {
    this.cacheService = cacheService;
    this.serverProperties = serverProperties;
    WebClient.Builder builder = Optional.ofNullable(builderProvider.getIfAvailable())
        .orElseGet(WebClient::builder);
    this.clientReference.set(builder.baseUrl(localBaseUrl()).build());
  }

  public void scheduleRevalidation(CacheMetadata metadata) {
    if (metadata == null || !cacheService.isCacheEnabled()) {
      return;
    }
    if (cacheService.isRefreshInFlight(metadata)) {
      return;
    }
    cacheService.trackRefreshStart(metadata);
    dispatch(metadata.method(), metadata.canonicalPath(), metadata.canonicalQuery(), metadata.tenantId(),
        metadata.route() != null && metadata.route().isTenantScoped(), true)
        .doFinally(signal -> cacheService.trackRefreshComplete(metadata))
        .subscribe();
  }

  public Mono<Void> warmRoute(RouteCacheProperties route, String candidatePath, String tenantId) {
    if (route == null || !cacheService.isCacheEnabled() || !StringUtils.hasText(candidatePath)) {
      return Mono.empty();
    }
    HttpMethod method = route.getMethod() != null ? route.getMethod() : HttpMethod.GET;
    String path = candidatePath;
    String query = "-";
    int queryIndex = candidatePath.indexOf('?');
    if (queryIndex >= 0) {
      path = candidatePath.substring(0, queryIndex);
      query = candidatePath.substring(queryIndex + 1);
    }
    return dispatch(method, path, query, tenantId, route.isTenantScoped(), true);
  }

  private Mono<Void> dispatch(HttpMethod method,
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
    UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromPath(path);
    if (StringUtils.hasText(query) && !"-".equals(query)) {
      uriBuilder.query(query);
    }
    return client.method(verb)
        .uri(uriBuilder.build(true).toUri())
        .headers(headers -> {
          if (tenantScoped && StringUtils.hasText(tenantId)) {
            headers.add(HeaderNames.X_TENANT_ID, tenantId);
          }
          if (forceRefresh) {
            headers.add(HttpHeaders.CACHE_CONTROL, "max-age=0, no-cache");
          }
          headers.add(HttpHeaders.ACCEPT, "application/json");
        })
        .retrieve()
        .toBodilessEntity()
        .timeout(Duration.ofSeconds(10))
        .doOnError(ex -> LOGGER.debug("Background cache refresh failed for {} {}", verb, path, ex))
        .onErrorResume(ex -> Mono.empty())
        .then();
  }

  private String localBaseUrl() {
    Integer configuredPort = serverProperties.getPort();
    int port = (configuredPort == null || configuredPort == 0) ? 8000 : configuredPort;
    return "http://127.0.0.1:" + port;
  }
}
