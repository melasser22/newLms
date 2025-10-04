package com.ejada.gateway.filter;

import com.ejada.gateway.config.GatewayFanoutProperties;
import com.ejada.gateway.config.GatewayFanoutProperties.FanoutRoute;
import com.ejada.gateway.config.GatewayFanoutProperties.Target;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Gateway filter that performs asynchronous fan-out notifications for a configured route. The
 * filter forwards the original request to one or more secondary services without impacting the
 * primary request/response lifecycle.
 */
@Component
public class FanoutGatewayFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(FanoutGatewayFilter.class);

  private final GatewayFanoutProperties properties;
  private final WebClient.Builder webClientBuilder;
  private final Map<String, FanoutRoute> validatedRoutes = new ConcurrentHashMap<>();

  public FanoutGatewayFilter(GatewayFanoutProperties properties, WebClient.Builder webClientBuilder) {
    this.properties = Objects.requireNonNull(properties, "properties");
    this.webClientBuilder = Objects.requireNonNull(webClientBuilder, "webClientBuilder");
  }

  public GatewayFilter apply(String routeId) {
    return new RouteFanoutFilter(routeId);
  }

  private FanoutRoute resolveRoute(String routeId) {
    if (!properties.isEnabled()) {
      return null;
    }
    FanoutRoute cached = validatedRoutes.get(routeId);
    if (cached != null) {
      return cached;
    }
    Optional<FanoutRoute> optional = properties.findRoute(routeId);
    if (optional.isEmpty()) {
      return null;
    }
    FanoutRoute route = optional.get();
    route.validate(routeId);
    validatedRoutes.put(routeId, route);
    return route;
  }

  private class RouteFanoutFilter implements GatewayFilter {

    private final String routeId;

    RouteFanoutFilter(String routeId) {
      this.routeId = routeId;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
      FanoutRoute route = resolveRoute(routeId);
      if (route == null || route.getTargets().isEmpty()) {
        return chain.filter(exchange);
      }

      return cacheBody(exchange)
          .flatMap(body -> {
            ServerWebExchange mutatedExchange = (body.length == 0)
                ? exchange
                : decorateExchange(exchange, body);
            triggerFanout(mutatedExchange, routeId, route, body);
            return chain.filter(mutatedExchange);
          });
    }
  }

  private Mono<byte[]> cacheBody(ServerWebExchange exchange) {
    return DataBufferUtils.join(exchange.getRequest().getBody())
        .map(dataBuffer -> {
          byte[] bytes = new byte[dataBuffer.readableByteCount()];
          dataBuffer.read(bytes);
          DataBufferUtils.release(dataBuffer);
          return bytes;
        })
        .defaultIfEmpty(new byte[0]);
  }

  private ServerWebExchange decorateExchange(ServerWebExchange exchange, byte[] body) {
    ServerHttpRequest request = exchange.getRequest();
    Flux<DataBuffer> cachedBody = Flux.defer(() -> {
      DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body);
      return Mono.just(buffer);
    });

    ServerHttpRequestDecorator decorated = new ServerHttpRequestDecorator(request) {
      @Override
      public Flux<DataBuffer> getBody() {
        return cachedBody;
      }

      @Override
      public HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.putAll(super.getHeaders());
        headers.remove(HttpHeaders.CONTENT_LENGTH);
        headers.setContentLength(body.length);
        return headers;
      }
    };

    return exchange.mutate().request(decorated).build();
  }

  private void triggerFanout(ServerWebExchange exchange,
      String routeId,
      FanoutRoute route,
      byte[] body) {
    Flux.fromIterable(route.getTargets())
        .flatMap(target -> invokeTarget(exchange, routeId, target, body)
            .onErrorResume(ex -> {
              LOGGER.warn("Fan-out target {} for route {} failed: {}", target.getId(), routeId, ex.toString());
              return Mono.empty();
            }))
        .subscribeOn(Schedulers.boundedElastic())
        .subscribe();
  }

  private Mono<Void> invokeTarget(ServerWebExchange exchange,
      String routeId,
      Target target,
      byte[] body) {
    WebClient.RequestBodySpec spec = webClientBuilder.clone()
        .build()
        .method(target.getMethod())
        .uri(target.getUri());

    HttpHeaders originalHeaders = filteredHeaders(exchange.getRequest().getHeaders());
    spec.headers(httpHeaders -> {
      originalHeaders.forEach((name, values) -> values.forEach(value -> httpHeaders.add(name, value)));
      if (!CollectionUtils.isEmpty(target.getHeaders())) {
        target.getHeaders().forEach(httpHeaders::set);
      }
    });

    boolean hasBody = body.length > 0 && allowsBody(target.getMethod());
    if (hasBody) {
      spec.body(BodyInserters.fromValue(body));
    }

    return spec.exchangeToMono(response -> response.releaseBody())
        .timeout(Duration.ofSeconds(5))
        .onErrorResume(ex -> {
          LOGGER.warn("Fan-out invocation {} for route {} failed", target.getId(), routeId, ex);
          return Mono.empty();
        });
  }

  private HttpHeaders filteredHeaders(HttpHeaders source) {
    HttpHeaders filtered = new HttpHeaders();
    source.forEach((name, values) -> {
      if (HttpHeaders.HOST.equalsIgnoreCase(name) || HttpHeaders.CONTENT_LENGTH.equalsIgnoreCase(name)) {
        return;
      }
      filtered.put(name, new ArrayList<>(values));
    });
    return filtered;
  }

  private boolean allowsBody(HttpMethod method) {
    if (method == null) {
      return false;
    }
    return switch (method) {
      case GET, HEAD, OPTIONS, TRACE -> false;
      default -> true;
    };
  }
}

