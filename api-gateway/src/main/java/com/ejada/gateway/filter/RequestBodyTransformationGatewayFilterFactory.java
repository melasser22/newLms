package com.ejada.gateway.filter;

import com.ejada.gateway.config.GatewayLimitsProperties;
import com.ejada.gateway.config.GatewayTransformationProperties.HeaderOperations;
import com.ejada.gateway.metrics.GatewayMetrics;
import com.ejada.gateway.transformation.HeaderTransformationService;
import com.ejada.gateway.transformation.RequestBodyTransformer;
import com.ejada.gateway.transformation.TransformationRuleCache;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.util.unit.DataSize;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Gateway filter that applies JSON request transformations and enforces size limits.
 */
public class RequestBodyTransformationGatewayFilterFactory {

  private final RequestBodyTransformer requestBodyTransformer;

  private final HeaderTransformationService headerTransformationService;

  private final GatewayLimitsProperties limitsProperties;

  private final TransformationRuleCache ruleCache;

  public RequestBodyTransformationGatewayFilterFactory(TransformationRuleCache ruleCache,
      HeaderTransformationService headerTransformationService,
      GatewayLimitsProperties limitsProperties,
      GatewayMetrics metrics) {
    this.ruleCache = Objects.requireNonNull(ruleCache, "ruleCache");
    this.requestBodyTransformer = new RequestBodyTransformer(ruleCache, metrics);
    this.headerTransformationService = Objects.requireNonNull(headerTransformationService, "headerTransformationService");
    this.limitsProperties = Objects.requireNonNull(limitsProperties, "limitsProperties");
  }

  public GatewayFilter apply(String routeId) {
    return new RequestBodyTransformationGatewayFilter(routeId);
  }

  private class RequestBodyTransformationGatewayFilter implements GatewayFilter, Ordered {

    private final String routeId;

    RequestBodyTransformationGatewayFilter(String routeId) {
      this.routeId = routeId;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
      Route route = exchange.getAttribute(org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
      String effectiveRouteId = (route != null) ? route.getId() : routeId;

      HeaderOperations headerOperations = headerTransformationService.resolveRequestOperations(effectiveRouteId);
      DataSize maxSize = limitsProperties.resolveMaxSize(effectiveRouteId);

      ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
          .headers(httpHeaders -> headerTransformationService.applyRequestHeaders(httpHeaders, headerOperations))
          .build();

      ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();

      boolean hasBodyRules = !ruleCache.resolveRequestRules(effectiveRouteId).isEmpty();
      boolean shouldTransformBody = hasBodyRules && shouldTransformBody(mutatedRequest.getHeaders().getContentType());

      if (maxSize != null) {
        long contentLength = mutatedRequest.getHeaders().getContentLength();
        if (contentLength > 0 && contentLength > maxSize.toBytes()) {
          return rejectLargeRequest(mutatedExchange);
        }
      }

      if (!shouldTransformBody && maxSize == null) {
        return chain.filter(mutatedExchange);
      }

      return DataBufferUtils.join(mutatedRequest.getBody())
          .flatMap(buffer -> {
            int readable = buffer.readableByteCount();
            if (maxSize != null && readable > maxSize.toBytes()) {
              DataBufferUtils.release(buffer);
              return rejectLargeRequest(mutatedExchange);
            }
            byte[] bodyBytes = new byte[readable];
            buffer.read(bodyBytes);
            DataBufferUtils.release(buffer);

            byte[] transformed = bodyBytes;
            if (shouldTransformBody && readable > 0) {
              transformed = requestBodyTransformer.transform(effectiveRouteId, bodyBytes);
            }
            byte[] finalTransformed = transformed;

            DataBufferFactory bufferFactory = mutatedExchange.getResponse().bufferFactory();
            Flux<DataBuffer> newBody = Flux.defer(() -> {
              DataBuffer newBuffer = bufferFactory.wrap(finalTransformed);
              return Mono.just(newBuffer);
            });

            ServerHttpRequest decorated = new ServerHttpRequestDecorator(mutatedRequest) {
              @Override
              public Flux<DataBuffer> getBody() {
                return newBody;
              }
            };

            return chain.filter(mutatedExchange.mutate().request(decorated).build());
          })
          .switchIfEmpty(chain.filter(mutatedExchange));
    }

    private boolean shouldTransformBody(MediaType contentType) {
      if (contentType == null) {
        return true;
      }
      if (MediaType.APPLICATION_JSON.includes(contentType)) {
        return true;
      }
      String subtype = contentType.getSubtype();
      return subtype != null && subtype.endsWith("+json");
    }

    private Mono<Void> rejectLargeRequest(ServerWebExchange exchange) {
      exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE);
      exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
      byte[] payload = ("{" +
          "\"status\":\"ERROR\"," +
          "\"code\":\"ERR_REQUEST_TOO_LARGE\"," +
          "\"message\":\"Request body exceeds the configured maximum\"}")
          .getBytes(StandardCharsets.UTF_8);
      DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(payload);
      return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
      return Ordered.HIGHEST_PRECEDENCE + 50;
    }
  }

}

