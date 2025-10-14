package com.ejada.gateway.filter;

import com.ejada.gateway.cache.CacheRefreshService;
import com.ejada.gateway.config.GatewayTransformationProperties.HeaderOperations;
import com.ejada.gateway.context.GatewayRequestAttributes;
import com.ejada.gateway.transformation.HeaderTransformationService;
import com.ejada.gateway.transformation.ResponseBodyTransformer;
import com.ejada.gateway.transformation.ResponseCacheService;
import com.ejada.gateway.transformation.ResponseCacheService.CacheMetadata;
import com.ejada.gateway.transformation.ResponseCacheService.CacheResult;
import com.ejada.gateway.transformation.ResponseCacheService.CachedResponse;
import com.ejada.gateway.metrics.GatewayMetrics.CacheState;
import java.time.Instant;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Gateway filter that wraps responses, applies header changes, and integrates caching.
 */
public class ResponseBodyTransformationGatewayFilterFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(ResponseBodyTransformationGatewayFilterFactory.class);

  private final HeaderTransformationService headerTransformationService;

  private final ResponseBodyTransformer responseBodyTransformer;

  private final ResponseCacheService responseCacheService;

  private final CacheRefreshService cacheRefreshService;

  public ResponseBodyTransformationGatewayFilterFactory(HeaderTransformationService headerTransformationService,
      ResponseBodyTransformer responseBodyTransformer,
      ResponseCacheService responseCacheService,
      CacheRefreshService cacheRefreshService) {
    this.headerTransformationService = headerTransformationService;
    this.responseBodyTransformer = responseBodyTransformer;
    this.responseCacheService = responseCacheService;
    this.cacheRefreshService = cacheRefreshService;
  }

  public GatewayFilter apply(String routeId) {
    return new ResponseTransformationGatewayFilter(routeId);
  }

  private class ResponseTransformationGatewayFilter implements GatewayFilter, Ordered {

    private final String routeId;

    ResponseTransformationGatewayFilter(String routeId) {
      this.routeId = routeId;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
      Route route = exchange.getAttribute(org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
      String effectiveRouteId = (route != null) ? route.getId() : routeId;
      String overrideRouteId = exchange.getAttribute(GatewayRequestAttributes.API_VERSION_TRANSFORMATION_ROUTE);
      if (StringUtils.hasText(overrideRouteId)) {
        effectiveRouteId = overrideRouteId;
      }

      HeaderOperations responseHeaders = headerTransformationService.resolveResponseOperations(effectiveRouteId);

      long startNanos = System.nanoTime();
      exchange.getAttributes().put("gateway.response.start", startNanos);

      HttpMethod method = exchange.getRequest().getMethod();
      if (responseCacheService != null && shouldInvalidateCache(method)) {
        return responseCacheService.invalidate(effectiveRouteId, exchange)
            .then(chain.filter(exchange));
      }

      if (!isCacheCandidate(method)) {
        return proceedWithDecoration(exchange, chain, responseHeaders, startNanos, null);
      }

      boolean bypass = clientBypassesCache(exchange);
      CacheMetadata metadata = (responseCacheService != null)
          ? responseCacheService.buildMetadata(effectiveRouteId, exchange).orElse(null)
          : null;
      if (metadata == null) {
        return proceedWithDecoration(exchange, chain, responseHeaders, startNanos, null);
      }

      if (bypass) {
        return proceedWithDecoration(exchange, chain, responseHeaders, startNanos, metadata);
      }

      return responseCacheService.find(effectiveRouteId, exchange)
          .flatMap(result -> handleCacheResult(result, exchange, chain, responseHeaders, startNanos, metadata));
    }

    private Mono<Void> handleCacheResult(CacheResult result,
        ServerWebExchange exchange,
        GatewayFilterChain chain,
        HeaderOperations responseHeaders,
        long startNanos,
        CacheMetadata metadata) {
      if (result == null || !result.hasRoute()) {
        return proceedWithDecoration(exchange, chain, responseHeaders, startNanos, metadata);
      }

      if (result.state() == CacheState.NOT_MODIFIED && result.response() != null) {
        return writeNotModified(exchange, result, responseHeaders);
      }

      if (result.response() != null && result.state() != null) {
        boolean stale = result.state() == CacheState.STALE;
        if (stale && cacheRefreshService != null) {
          cacheRefreshService.scheduleRevalidation(result.metadata());
        }
        return writeCachedResponse(exchange, result, responseHeaders, stale);
      }

      return proceedWithDecoration(exchange, chain, responseHeaders, startNanos, result.metadata());
    }

    private Mono<Void> proceedWithDecoration(ServerWebExchange exchange,
        GatewayFilterChain chain,
        HeaderOperations responseHeaders,
        long startNanos,
        CacheMetadata metadata) {
      ServerHttpResponse originalResponse = exchange.getResponse();
      DataBufferFactory bufferFactory = originalResponse.bufferFactory();
      boolean cachingActive = metadata != null && responseCacheService != null && responseCacheService.isCacheEnabled();

      ServerHttpResponseDecorator decorated = new ServerHttpResponseDecorator(originalResponse) {
        @Override
        public Mono<Void> writeWith(org.reactivestreams.Publisher<? extends DataBuffer> body) {
          Flux<? extends DataBuffer> bodyFlux = Flux.from(body);
          return DataBufferUtils.join(bodyFlux)
              .flatMap(buffer -> {
                byte[] originalBytes = new byte[buffer.readableByteCount()];
                buffer.read(originalBytes);
                DataBufferUtils.release(buffer);

                HttpStatusCode statusCode = getStatusCode();
                HttpStatus status = statusCode != null ? HttpStatus.resolve(statusCode.value()) : null;
                MediaType contentType = getHeaders().getContentType();
                byte[] processed = originalBytes;

                if (responseBodyTransformer.isWrapEnabled(contentType)) {
                  processed = responseBodyTransformer.wrapBody(exchange, originalBytes, status, startNanos);
                }

                headerTransformationService.applyResponseHeaders(getHeaders(), responseHeaders);

                Instant capturedAt = Instant.now();
                String etag = null;
                if (cachingActive) {
                  etag = responseCacheService.generateEtag(processed);
                  responseCacheService.applyCacheHeaders(getHeaders(), metadata.route(), etag, capturedAt, false);
                  originalResponse.getHeaders().set("X-Cache", "MISS");
                }

                byte[] finalBytes = processed;
                getHeaders().setContentLength(finalBytes.length);
                DataBuffer wrap = bufferFactory.wrap(finalBytes);

                Mono<Void> writeMono = super.writeWith(Mono.just(wrap));

                if (cachingActive && isSuccessful(statusCode)) {
                  CachedResponse snapshot = responseCacheService.snapshotResponse(metadata.route(), this, processed, etag, capturedAt);
                  return responseCacheService.store(metadata, snapshot)
                      .onErrorResume(ex -> {
                        LOGGER.warn("Failed to store response in cache", ex);
                        return Mono.empty();
                      })
                      .then(writeMono);
                }
                return writeMono;
              })
              .switchIfEmpty(super.writeWith(body))
              .onErrorResume(ex -> {
                LOGGER.warn("Failed to process response body", ex);
                return super.writeWith(body);
              });
        }

        @Override
        public Mono<Void> writeAndFlushWith(org.reactivestreams.Publisher<? extends org.reactivestreams.Publisher<? extends DataBuffer>> body) {
          return writeWith(Flux.from(body).flatMapSequential(publisher -> publisher));
        }
      };

      exchange.getResponse().beforeCommit(() -> {
        headerTransformationService.applyResponseHeaders(exchange.getResponse().getHeaders(), responseHeaders);
        return Mono.empty();
      });

      return chain.filter(exchange.mutate().response(decorated).build());
    }

    private Mono<Void> writeCachedResponse(ServerWebExchange exchange,
        CacheResult result,
        HeaderOperations responseHeaders,
        boolean stale) {
      CachedResponse cachedResponse = result.response();
      CacheMetadata metadata = result.metadata();
      ServerHttpResponse response = exchange.getResponse();
      response.setStatusCode(HttpStatus.valueOf(cachedResponse.status()));
      HttpHeaders headers = new HttpHeaders();
      headers.putAll(cachedResponse.headers());
      responseCacheService.applyCacheHeaders(headers, metadata.route(), cachedResponse.etag(), cachedResponse.cachedAt(), stale);
      headerTransformationService.applyResponseHeaders(headers, responseHeaders);
      headers.set("X-Cache", stale ? "STALE" : "HIT");
      response.getHeaders().clear();
      response.getHeaders().putAll(headers);
      response.getHeaders().setContentLength(cachedResponse.body().length);
      DataBuffer buffer = response.bufferFactory().wrap(cachedResponse.body());
      return response.writeWith(Mono.just(buffer));
    }

    private Mono<Void> writeNotModified(ServerWebExchange exchange,
        CacheResult result,
        HeaderOperations responseHeaders) {
      CachedResponse cachedResponse = result.response();
      CacheMetadata metadata = result.metadata();
      ServerHttpResponse response = exchange.getResponse();
      response.setStatusCode(HttpStatus.NOT_MODIFIED);
      HttpHeaders headers = response.getHeaders();
      headers.clear();
      responseCacheService.applyCacheHeaders(headers, metadata.route(), cachedResponse.etag(), cachedResponse.cachedAt(), false);
      headerTransformationService.applyResponseHeaders(headers, responseHeaders);
      headers.set("X-Cache", "NOT_MODIFIED");
      return response.setComplete();
    }

    private boolean isCacheCandidate(HttpMethod method) {
      return responseCacheService != null
          && responseCacheService.isCacheEnabled()
          && HttpMethod.GET.equals(method);
    }

    private boolean shouldInvalidateCache(HttpMethod method) {
      if (method == null) {
        return false;
      }
      return method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.DELETE;
    }

    private boolean clientBypassesCache(ServerWebExchange exchange) {
      HttpHeaders headers = exchange.getRequest().getHeaders();
      String cacheControl = headers.getFirst(HttpHeaders.CACHE_CONTROL);
      if (cacheControl != null) {
        String lower = cacheControl.toLowerCase(Locale.ROOT);
        if (lower.contains("no-cache") || lower.contains("no-store") || lower.contains("max-age=0")) {
          return true;
        }
      }
      return headers.getOrEmpty(HttpHeaders.PRAGMA).stream()
          .map(value -> value.toLowerCase(Locale.ROOT))
          .anyMatch(value -> value.contains("no-cache"));
    }

    private boolean isSuccessful(HttpStatusCode status) {
      return status != null && status.is2xxSuccessful();
    }

    @Override
    public int getOrder() {
      return Ordered.LOWEST_PRECEDENCE - 50;
    }
  }
}
