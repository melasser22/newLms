package com.ejada.gateway.filter;

import com.ejada.gateway.transformation.ResponseCacheService;
import com.ejada.gateway.transformation.ResponseCacheService.CacheMetadata;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

/**
 * Deduplicates in-flight GET requests targeting the same cacheable resource so a
 * single downstream call satisfies concurrent gateway clients.
 */
@Component
public class ReactiveRequestCoalescingFilter implements GlobalFilter, Ordered {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReactiveRequestCoalescingFilter.class);

  private final ResponseCacheService cacheService;

  private final Sinks.Many<CoalescedRequest> requestSink;

  private final ConcurrentMap<String, Mono<Void>> inFlight = new ConcurrentHashMap<>();

  public ReactiveRequestCoalescingFilter(ResponseCacheService cacheService) {
    this.cacheService = cacheService;
    this.requestSink = Sinks.many().multicast().onBackpressureBuffer();
    this.requestSink.asFlux()
        .groupBy(CoalescedRequest::key)
        .flatMap(group -> group.concatMap(this::process))
        .subscribe();
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    if (!cacheService.isCacheEnabled() || !isCoalescable(exchange)) {
      return chain.filter(exchange);
    }
    String routeId = resolveRouteId(exchange);
    CacheMetadata metadata = cacheService.buildMetadata(routeId, exchange).orElse(null);
    if (metadata == null) {
      return chain.filter(exchange);
    }
    if (clientBypassesCache(exchange.getRequest().getHeaders())) {
      return chain.filter(exchange);
    }
    String key = metadata.cacheKey();
    Sinks.One<Void> completion = Sinks.one();
    Sinks.EmitResult result = requestSink.tryEmitNext(new CoalescedRequest(key, metadata, exchange, chain, completion));
    if (result.isFailure()) {
      LOGGER.debug("Failed to emit coalesced request for key {} due to {}", key, result);
      return chain.filter(exchange);
    }
    return completion.asMono();
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 10;
  }

  private Mono<Void> process(CoalescedRequest request) {
    Mono<Void> cached = inFlight.computeIfAbsent(request.key(), key ->
        Mono.defer(() -> request.chain().filter(request.exchange()))
            .doFinally(signal -> inFlight.remove(key))
            .cache());
    return cached
        .doOnError(request.completion()::tryEmitError)
        .doOnSuccess(ignored -> request.completion().tryEmitEmpty())
        .doOnCancel(() -> request.completion().tryEmitEmpty())
        .onErrorResume(ex -> Mono.empty());
  }

  private boolean isCoalescable(ServerWebExchange exchange) {
    HttpMethod method = exchange.getRequest().getMethod();
    if (method != HttpMethod.GET) {
      return false;
    }
    return true;
  }

  private boolean clientBypassesCache(HttpHeaders headers) {
    if (headers == null) {
      return false;
    }
    String cacheControl = headers.getFirst(HttpHeaders.CACHE_CONTROL);
    if (cacheControl != null) {
      String lower = cacheControl.toLowerCase();
      if (lower.contains("no-cache") || lower.contains("no-store") || lower.contains("max-age=0")) {
        return true;
      }
    }
    return headers.getOrEmpty(HttpHeaders.PRAGMA).stream()
        .map(String::toLowerCase)
        .anyMatch(value -> value.contains("no-cache"));
  }

  private String resolveRouteId(ServerWebExchange exchange) {
    Object routeAttr = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
    if (routeAttr instanceof org.springframework.cloud.gateway.route.Route route) {
      return route.getId();
    }
    return null;
  }

  private record CoalescedRequest(String key,
                                  CacheMetadata metadata,
                                  ServerWebExchange exchange,
                                  GatewayFilterChain chain,
                                  Sinks.One<Void> completion) {
  }
}
