package com.ejada.gateway.security;

import com.ejada.common.constants.HeaderNames;
import com.ejada.common.dto.BaseResponse;
import com.ejada.gateway.config.GatewaySecurityProperties;
import com.ejada.gateway.context.GatewayRequestAttributes;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Objects;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Validates HMAC request signatures for sensitive operations.
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 15)
public class RequestSignatureValidationFilter implements WebFilter, Ordered {

  private static final Logger LOGGER = LoggerFactory.getLogger(RequestSignatureValidationFilter.class);
  private static final AntPathMatcher ANT_PATH_MATCHER = new AntPathMatcher();
  private static final String SIGNATURE_HEADER = "X-Signature";

  private final ReactiveStringRedisTemplate redisTemplate;
  private final GatewaySecurityProperties properties;
  private final GatewaySecurityMetrics metrics;
  private final ObjectMapper objectMapper;

  public RequestSignatureValidationFilter(
      ReactiveStringRedisTemplate redisTemplate,
      GatewaySecurityProperties properties,
      GatewaySecurityMetrics metrics,
      @Qualifier("jacksonObjectMapper") ObjectProvider<ObjectMapper> primaryObjectMapper,
      ObjectProvider<ObjectMapper> fallbackObjectMapper) {
    this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate");
    this.properties = Objects.requireNonNull(properties, "properties");
    this.metrics = Objects.requireNonNull(metrics, "metrics");
    ObjectMapper mapper = (primaryObjectMapper != null) ? primaryObjectMapper.getIfAvailable() : null;
    if (mapper == null) {
      mapper = (fallbackObjectMapper != null) ? fallbackObjectMapper.getIfAvailable() : null;
    }
    this.objectMapper = Objects.requireNonNull(mapper, "objectMapper");
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    if (!properties.getSignatureValidation().isEnabled()) {
      return chain.filter(exchange);
    }

    if (shouldSkip(exchange)) {
      return chain.filter(exchange);
    }

    String tenantId = resolveTenant(exchange);
    if (!StringUtils.hasText(tenantId)) {
      metrics.incrementBlocked("signature", null);
      return reject(exchange, HttpStatus.UNAUTHORIZED, "ERR_SIGNATURE_TENANT", "Missing tenant context for signature validation");
    }

    String providedSignature = trimToNull(exchange.getRequest().getHeaders().getFirst(SIGNATURE_HEADER));
    if (!StringUtils.hasText(providedSignature)) {
      metrics.incrementBlocked("signature", tenantId);
      return reject(exchange, HttpStatus.UNAUTHORIZED, "ERR_SIGNATURE_MISSING", "Missing request signature");
    }

    return DataBufferUtils.join(exchange.getRequest().getBody())
        .defaultIfEmpty(exchange.getResponse().bufferFactory().wrap(new byte[0]))
        .flatMap(buffer -> {
          byte[] body = new byte[buffer.readableByteCount()];
          buffer.read(body);
          DataBufferUtils.release(buffer);
          return redisTemplate.opsForValue()
              .get(properties.getSignatureValidation().redisKey(tenantId))
              .switchIfEmpty(Mono.defer(() -> {
                metrics.incrementBlocked("signature", tenantId);
                return reject(exchange, HttpStatus.UNAUTHORIZED, "ERR_SIGNATURE_SECRET", "Signature secret not found")
                    .then(Mono.empty());
              }))
              .flatMap(secret -> verifyAndContinue(secret, providedSignature, tenantId, body, exchange, chain));
        });
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 15;
  }

  private Mono<Void> verifyAndContinue(String secret, String providedSignature, String tenantId,
      byte[] body, ServerWebExchange exchange, WebFilterChain chain) {
    try {
      String payload = buildPayload(exchange.getRequest(), body);
      String computed = computeHmac(secret, payload);
      if (!MessageDigest.isEqual(computed.getBytes(StandardCharsets.UTF_8),
          providedSignature.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8))) {
        LOGGER.debug("Signature mismatch for tenant {}", tenantId);
        metrics.incrementBlocked("signature", tenantId);
        return reject(exchange, HttpStatus.UNAUTHORIZED, "ERR_SIGNATURE_INVALID", "Invalid request signature");
      }
      ServerHttpRequest decoratedRequest = decorateRequest(exchange, body);
      ServerWebExchange mutated = exchange.mutate().request(decoratedRequest).build();
      return chain.filter(mutated);
    } catch (Exception ex) {
      LOGGER.warn("Failed to validate signature for tenant {}", tenantId, ex);
      metrics.incrementBlocked("signature", tenantId);
      return reject(exchange, HttpStatus.UNAUTHORIZED, "ERR_SIGNATURE_INVALID", "Invalid request signature");
    }
  }

  private boolean shouldSkip(ServerWebExchange exchange) {
    HttpMethod method = exchange.getRequest().getMethod();
    if (method == null || HttpMethod.GET.equals(method) || HttpMethod.HEAD.equals(method) || HttpMethod.OPTIONS.equals(method)) {
      return true;
    }
    String path = exchange.getRequest().getPath().value();
    for (String pattern : properties.getSignatureValidation().getSkipPatterns()) {
      if (StringUtils.hasText(pattern) && ANT_PATH_MATCHER.match(pattern, path)) {
        return true;
      }
    }
    return false;
  }

  private String resolveTenant(ServerWebExchange exchange) {
    String tenant = exchange.getAttribute(GatewayRequestAttributes.TENANT_ID);
    if (!StringUtils.hasText(tenant)) {
      tenant = exchange.getRequest().getHeaders().getFirst(HeaderNames.X_TENANT_ID);
    }
    return trimToNull(tenant);
  }

  private ServerHttpRequest decorateRequest(ServerWebExchange exchange, byte[] body) {
    DataBufferFactory bufferFactory = exchange.getResponse().bufferFactory();
    return new ServerHttpRequestDecorator(exchange.getRequest(), body, bufferFactory);
  }

  private String buildPayload(ServerHttpRequest request, byte[] body) {
    HttpMethod methodEnum = request.getMethod();
    String method = methodEnum != null ? methodEnum.name() : "UNKNOWN";
    String path = request.getURI().getRawPath();
    String query = request.getURI().getRawQuery();
    return method + "\n" + path + "\n" + (query != null ? query : "") + "\n" + new String(body, StandardCharsets.UTF_8);
  }

  private String computeHmac(String secret, String payload) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
    StringBuilder sb = new StringBuilder(digest.length * 2);
    for (byte b : digest) {
      sb.append(String.format(Locale.ROOT, "%02x", b));
    }
    return sb.toString();
  }

  private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, String code, String message) {
    var response = exchange.getResponse();
    if (response.isCommitted()) {
      LOGGER.debug("Response already committed, skipping request signature rejection for {}", exchange.getRequest().getPath());
      return Mono.empty();
    }
    response.setStatusCode(status);
    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
    BaseResponse<Void> body = BaseResponse.error(code, message);
    byte[] payload;
    try {
      payload = objectMapper.writeValueAsBytes(body);
    } catch (Exception ex) {
      payload = body.toString().getBytes(StandardCharsets.UTF_8);
    }
    return response.writeWith(Mono.just(response.bufferFactory().wrap(payload)));
  }

  private static String trimToNull(@Nullable String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private static final class ServerHttpRequestDecorator extends org.springframework.http.server.reactive.ServerHttpRequestDecorator {

    private final byte[] cachedBody;
    private final DataBufferFactory bufferFactory;

    private ServerHttpRequestDecorator(ServerHttpRequest delegate, byte[] cachedBody, DataBufferFactory bufferFactory) {
      super(delegate);
      this.cachedBody = cachedBody;
      this.bufferFactory = bufferFactory;
    }

    @Override
    public Flux<DataBuffer> getBody() {
      return Flux.defer(() -> {
        byte[] copy = new byte[cachedBody.length];
        System.arraycopy(cachedBody, 0, copy, 0, cachedBody.length);
        return Flux.just(bufferFactory.wrap(copy));
      });
    }
  }
}
