package com.ejada.gateway.filter;

import com.ejada.gateway.config.GatewayTransformationProperties.HeaderOperations;
import com.ejada.gateway.transformation.HeaderTransformationService;
import com.ejada.gateway.transformation.ResponseBodyTransformer;
import com.ejada.gateway.transformation.ResponseCacheService;
import com.ejada.gateway.transformation.ResponseCacheService.CachedResponse;
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
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
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

  public ResponseBodyTransformationGatewayFilterFactory(HeaderTransformationService headerTransformationService,
      ResponseBodyTransformer responseBodyTransformer,
      ResponseCacheService responseCacheService) {
    this.headerTransformationService = headerTransformationService;
    this.responseBodyTransformer = responseBodyTransformer;
    this.responseCacheService = responseCacheService;
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

      HeaderOperations responseHeaders = headerTransformationService.resolveResponseOperations(effectiveRouteId);

      long startNanos = System.nanoTime();
      exchange.getAttributes().put("gateway.response.start", startNanos);

      if (responseCacheService != null && shouldInvalidateCache(exchange.getRequest().getMethod())) {
        return responseCacheService.invalidate(effectiveRouteId, exchange)
            .then(chain.filter(exchange));
      }

      if (isCacheable(exchange)) {
        return responseCacheService.find(effectiveRouteId, exchange)
            .flatMap(optional -> optional
                .map(cached -> writeCachedResponse(exchange, cached, responseHeaders))
                .orElseGet(() -> proceedWithDecoration(exchange, chain, effectiveRouteId, responseHeaders, startNanos, true)))
            .switchIfEmpty(proceedWithDecoration(exchange, chain, effectiveRouteId, responseHeaders, startNanos, true));
      }

      return proceedWithDecoration(exchange, chain, effectiveRouteId, responseHeaders, startNanos, false);
    }

    private Mono<Void> proceedWithDecoration(ServerWebExchange exchange,
        GatewayFilterChain chain,
        String effectiveRouteId,
        HeaderOperations responseHeaders,
        long startNanos,
        boolean cacheEligible) {
      ServerHttpResponse originalResponse = exchange.getResponse();
      DataBufferFactory bufferFactory = originalResponse.bufferFactory();
      boolean cachingActive = cacheEligible && responseCacheService != null && responseCacheService.isCacheEnabled();

      ServerHttpResponseDecorator decorated = new ServerHttpResponseDecorator(originalResponse) {
        @Override
        public Mono<Void> writeWith(org.reactivestreams.Publisher<? extends DataBuffer> body) {
          Flux<? extends DataBuffer> bodyFlux = Flux.from(body);
          return DataBufferUtils.join(bodyFlux)
              .flatMap(buffer -> {
                byte[] originalBytes = new byte[buffer.readableByteCount()];
                buffer.read(originalBytes);
                DataBufferUtils.release(buffer);

                HttpStatus status = getStatusCode();
                MediaType contentType = getHeaders().getContentType();
                byte[] processed = originalBytes;

                if (responseBodyTransformer.isWrapEnabled(contentType)) {
                  processed = responseBodyTransformer.wrapBody(exchange, originalBytes, status, startNanos);
                }

                headerTransformationService.applyResponseHeaders(getHeaders(), responseHeaders);

                if (cachingActive) {
                  originalResponse.getHeaders().set("X-Cache", "MISS");
                }

                byte[] finalBytes = processed;
                getHeaders().setContentLength(finalBytes.length);
                DataBuffer wrap = bufferFactory.wrap(finalBytes);

                Mono<Void> writeMono = super.writeWith(Mono.just(wrap));

                if (cachingActive && isSuccessful(status)) {
                  CachedResponse snapshot = responseCacheService.snapshotResponse(this, processed);
                  return responseCacheService.store(effectiveRouteId, exchange, snapshot)
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
        CachedResponse cachedResponse,
        HeaderOperations responseHeaders) {
      ServerHttpResponse response = exchange.getResponse();
      response.setStatusCode(HttpStatus.valueOf(cachedResponse.status()));
      HttpHeaders headers = response.getHeaders();
      headers.clear();
      headers.putAll(cachedResponse.headers());
      headerTransformationService.applyResponseHeaders(headers, responseHeaders);
      headers.set("X-Cache", "HIT");
      headers.setContentLength(cachedResponse.body().length);
      DataBuffer buffer = response.bufferFactory().wrap(cachedResponse.body());
      return response.writeWith(Mono.just(buffer));
    }

    private boolean isCacheable(ServerWebExchange exchange) {
      return responseCacheService != null
          && responseCacheService.isCacheEnabled()
          && HttpMethod.GET.equals(exchange.getRequest().getMethod());
    }

    private boolean shouldInvalidateCache(HttpMethod method) {
      if (method == null) {
        return false;
      }
      return method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.DELETE;
    }

    private boolean isSuccessful(HttpStatus status) {
      return status != null && status.is2xxSuccessful();
    }

    @Override
    public int getOrder() {
      return Ordered.LOWEST_PRECEDENCE - 50;
    }
  }
}

