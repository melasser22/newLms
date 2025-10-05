package com.ejada.gateway.ratelimit;

import com.ejada.common.constants.HeaderNames;
import com.ejada.common.context.ContextManager;
import com.ejada.common.dto.BaseResponse;
import com.ejada.gateway.config.GatewayRateLimitProperties;
import com.ejada.gateway.config.GatewayRateLimitProperties.TierLimit;
import com.ejada.gateway.context.GatewayRequestAttributes;
import com.ejada.gateway.observability.GatewayTracingHelper;
import com.ejada.shared_starter_ratelimit.RateLimitProps;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Reactive adaptation of the servlet {@code RateLimitFilter}. Supports fixed and sliding
 * window algorithms with optional burst handling and tier-based limits.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class ReactiveRateLimiterFilter implements WebFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReactiveRateLimiterFilter.class);
  private static final String DEFAULT_FALLBACK_KEY = "anonymous";
  private static final String INTERNAL_HEADER = "X-Internal-Request";
  private static final long INTERNAL_REQUEST_MAX_SKEW_SECONDS = 300;

  private static final String LUA_RATE_LIMIT_SCRIPT = """
local algorithm = ARGV[1]
local now = tonumber(ARGV[2])
local windowMillis = tonumber(ARGV[3])
local capacity = tonumber(ARGV[4])
local burstCapacity = tonumber(ARGV[5])
local burstTtl = tonumber(ARGV[6])
local extraBurst = math.max(0, burstCapacity - capacity)
local baseCount = 0
local allowed = 0
local burstUsed = 0
local totalRemaining = 0
local baseRemaining = 0
local burstRemaining = 0
local resetTimestamp = now + windowMillis
local windowSeconds = windowMillis / 1000

if algorithm == 'sliding' then
  redis.call('ZREMRANGEBYSCORE', KEYS[1], '-inf', now - windowMillis)
  baseCount = redis.call('ZCOUNT', KEYS[1], now - windowMillis, now)
else
  local value = redis.call('GET', KEYS[1])
  if value then
    baseCount = tonumber(value)
  else
    baseCount = 0
  end
end

baseRemaining = math.max(0, capacity - baseCount)
local burstUsage = tonumber(redis.call('GET', KEYS[2]) or '0')
burstRemaining = math.max(0, extraBurst - burstUsage)

if baseCount < capacity then
  allowed = 1
  if algorithm == 'sliding' then
    redis.call('ZADD', KEYS[1], now, now)
    redis.call('PEXPIRE', KEYS[1], windowMillis)
    baseCount = redis.call('ZCOUNT', KEYS[1], now - windowMillis, now)
    local oldest = redis.call('ZRANGE', KEYS[1], 0, 0, 'WITHSCORES')
    if oldest[2] then
      resetTimestamp = tonumber(oldest[2]) + windowMillis
    end
  else
    baseCount = redis.call('INCR', KEYS[1])
    if baseCount == 1 then
      redis.call('PEXPIRE', KEYS[1], windowMillis)
    end
    resetTimestamp = now + windowMillis
  end
elseif extraBurst > 0 and burstUsage < extraBurst then
  allowed = 1
  burstUsed = 1
  burstUsage = redis.call('INCR', KEYS[2])
  if burstUsage == 1 then
    redis.call('PEXPIRE', KEYS[2], burstTtl)
  end
  burstRemaining = math.max(0, extraBurst - burstUsage)
  if algorithm == 'sliding' then
    redis.call('ZADD', KEYS[1], now, now)
    redis.call('PEXPIRE', KEYS[1], windowMillis)
    baseCount = redis.call('ZCOUNT', KEYS[1], now - windowMillis, now)
    local oldest = redis.call('ZRANGE', KEYS[1], 0, 0, 'WITHSCORES')
    if oldest[2] then
      resetTimestamp = tonumber(oldest[2]) + windowMillis
    end
  else
    baseCount = redis.call('INCR', KEYS[1])
    if baseCount == 1 then
      redis.call('PEXPIRE', KEYS[1], windowMillis)
    end
    resetTimestamp = now + windowMillis
  end
else
  allowed = 0
  if algorithm == 'sliding' then
    local oldest = redis.call('ZRANGE', KEYS[1], 0, 0, 'WITHSCORES')
    if oldest[2] then
      resetTimestamp = tonumber(oldest[2]) + windowMillis
    end
  end
end

baseRemaining = math.max(0, capacity - baseCount)
totalRemaining = baseRemaining + burstRemaining

return {tostring(allowed), tostring(totalRemaining), tostring(resetTimestamp), tostring(windowSeconds), tostring(burstUsed), tostring(baseRemaining), tostring(burstRemaining)}
""";

  private static final RedisScript<List> RATE_LIMIT_SCRIPT = new DefaultRedisScript<>(
      LUA_RATE_LIMIT_SCRIPT, List.class);

  private final ReactiveStringRedisTemplate redisTemplate;
  private final RateLimitProps props;
  private final KeyResolver keyResolver;
  private final ObjectMapper objectMapper;
  private final GatewayRateLimitProperties gatewayProps;
  private final MeterRegistry meterRegistry;
  private final Counter bypassCounter;
  private final Counter burstCounter;
  private final GatewayTracingHelper tracingHelper;

  @Autowired
  public ReactiveRateLimiterFilter(ReactiveStringRedisTemplate redisTemplate,
      RateLimitProps props,
      KeyResolver keyResolver,
      @Qualifier("jacksonObjectMapper") ObjectProvider<ObjectMapper> jacksonObjectMapper,
      ObjectProvider<ObjectMapper> objectMapperProvider,
      ObjectProvider<GatewayTracingHelper> tracingHelperProvider) {
    this(redisTemplate, props, keyResolver,
        resolveObjectMapper(jacksonObjectMapper, objectMapperProvider),
        new GatewayRateLimitProperties(), null, tracingHelperProvider.getIfAvailable());
  }

  public ReactiveRateLimiterFilter(ReactiveStringRedisTemplate redisTemplate,
      RateLimitProps props,
      KeyResolver keyResolver,
      @Nullable ObjectMapper objectMapper) {
    this(redisTemplate, props, keyResolver, objectMapper, new GatewayRateLimitProperties(), null,
        null);
  }

  public ReactiveRateLimiterFilter(ReactiveStringRedisTemplate redisTemplate,
      RateLimitProps props,
      KeyResolver keyResolver,
      @Nullable ObjectMapper objectMapper,
      @Nullable GatewayRateLimitProperties gatewayProps,
      @Nullable MeterRegistry meterRegistry,
      @Nullable GatewayTracingHelper tracingHelper) {
    this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate");
    this.props = Objects.requireNonNull(props, "props");
    this.keyResolver = Objects.requireNonNull(keyResolver, "keyResolver");
    this.objectMapper = objectMapper;
    this.gatewayProps = (gatewayProps != null) ? gatewayProps : new GatewayRateLimitProperties();
    this.meterRegistry = meterRegistry;
    this.bypassCounter = (meterRegistry != null)
        ? meterRegistry.counter("gateway.ratelimit.bypass_count")
        : null;
    this.burstCounter = (meterRegistry != null)
        ? meterRegistry.counter("gateway.ratelimit.burst_used")
        : null;
    this.tracingHelper = tracingHelper;
  }

  @Override
  @Timed(value = "gateway.ratelimit.filter", description = "Rate limiter filter execution time")
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    return resolveKey(exchange)
        .flatMap(key -> applyRateLimit(exchange, chain, key))
        .onErrorResume(ex -> {
          LOGGER.warn("Rate limiter failed, allowing request", ex);
          return chain.filter(exchange);
        });
  }

  private Mono<Void> applyRateLimit(ServerWebExchange exchange, WebFilterChain chain, String key) {
    if (shouldBypass(exchange)) {
      recordBypass();
      return chain.filter(exchange);
    }

    LimitDefinition limit = resolveLimit(exchange);
    return executeRateLimit(key, limit)
        .flatMap(decision -> {
          applyHeaders(exchange, limit, decision);
          recordRateLimitDecision(exchange, limit, decision);
          if (!decision.allowed()) {
            return reject(exchange);
          }
          if (decision.burstConsumed()) {
            recordBurstUsage();
          }
          return chain.filter(exchange);
        });
  }

  private Mono<RateLimitDecision> executeRateLimit(String key, LimitDefinition limit) {
    Duration window = limit.window();
    String algorithm = resolveAlgorithm();
    Instant now = Instant.now();
    List<String> keys = List.of(rateKey(key, algorithm), burstKey(key));
    List<String> args = List.of(
        algorithm,
        String.valueOf(now.toEpochMilli()),
        String.valueOf(window.toMillis()),
        String.valueOf(limit.capacity()),
        String.valueOf(limit.burstCapacity()),
        String.valueOf(window.toMillis()));

    return redisTemplate.execute(RATE_LIMIT_SCRIPT, keys, args)
        .next()
        .map(result -> decodeResult(result, limit, window, now))
        .defaultIfEmpty(defaultDecision(limit, window, now));
  }

  private RateLimitDecision decodeResult(List<?> raw, LimitDefinition limit, Duration window, Instant now) {
    if (raw == null || raw.size() < 5) {
      return defaultDecision(limit, window, now);
    }
    boolean allowed = parseBoolean(raw.get(0));
    long remaining = parseLong(raw.get(1), limit.burstCapacity());
    long resetMillis = parseLong(raw.get(2), now.plus(window).toEpochMilli());
    boolean burstUsed = parseBoolean(raw.get(4));
    long baseRemaining = parseLong(raw.size() > 5 ? raw.get(5) : null, Math.max(0, limit.capacity()));
    long burstRemaining = parseLong(raw.size() > 6 ? raw.get(6) : null,
        Math.max(0, limit.burstCapacity() - limit.capacity()));
    long totalRemaining = Math.max(0, Math.min(remaining, baseRemaining + burstRemaining));
    return new RateLimitDecision(allowed, totalRemaining, resetMillis, window, burstUsed,
        baseRemaining, burstRemaining);
  }

  private RateLimitDecision defaultDecision(LimitDefinition limit, Duration window, Instant now) {
    long totalRemaining = Math.max(0, limit.burstCapacity());
    long baseRemaining = Math.max(0, limit.capacity());
    long burstRemaining = Math.max(0, limit.burstCapacity() - limit.capacity());
    return new RateLimitDecision(true, totalRemaining, now.plus(window).toEpochMilli(), window,
        false, baseRemaining, burstRemaining);
  }

  private void applyHeaders(ServerWebExchange exchange, LimitDefinition limit, RateLimitDecision decision) {
    long windowSeconds = Math.max(1L, Math.round(Math.ceil((double) limit.window().toMillis() / 1000d)));
    long resetSeconds = Math.max(0L, decision.resetEpochMillis() / 1000L);
    String policy = StringUtils.hasText(limit.policy()) ? limit.policy() : "default";
    exchange.getResponse().getHeaders().set("X-RateLimit-Limit", String.valueOf(limit.capacity()));
    exchange.getResponse().getHeaders().set("X-RateLimit-Remaining", String.valueOf(decision.remaining()));
    exchange.getResponse().getHeaders().set("X-RateLimit-Reset", String.valueOf(resetSeconds));
    exchange.getResponse().getHeaders().set("X-RateLimit-Window", String.valueOf(windowSeconds));
    exchange.getResponse().getHeaders().set("X-RateLimit-Policy", policy);
  }

  private boolean shouldBypass(ServerWebExchange exchange) {
    if (gatewayProps == null || !gatewayProps.isAllowInternalBypass()) {
      return false;
    }
    String secret = gatewayProps.getInternalSharedSecret();
    if (!StringUtils.hasText(secret)) {
      return false;
    }
    String header = exchange.getRequest().getHeaders().getFirst(INTERNAL_HEADER);
    if (!StringUtils.hasText(header)) {
      return false;
    }
    return verifyInternalRequest(header, secret, exchange);
  }

  private boolean verifyInternalRequest(String header, String secret, ServerWebExchange exchange) {
    try {
      String[] parts = header.split(":", 2);
      if (parts.length != 2) {
        return false;
      }
      String timestampPart = parts[0].trim();
      String signaturePart = parts[1].trim();
      if (!StringUtils.hasText(timestampPart) || !StringUtils.hasText(signaturePart)) {
        return false;
      }
      long timestamp = Long.parseLong(timestampPart);
      long now = Instant.now().getEpochSecond();
      if (Math.abs(now - timestamp) > INTERNAL_REQUEST_MAX_SKEW_SECONDS) {
        return false;
      }
      String method = exchange.getRequest().getMethod() != null
          ? exchange.getRequest().getMethod().name()
          : "UNKNOWN";
      String payload = timestampPart + ":" + method + ":"
          + exchange.getRequest().getPath().value();
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      String expected = Base64.getEncoder().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
      return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
          signaturePart.getBytes(StandardCharsets.UTF_8));
    } catch (Exception ex) {
      LOGGER.debug("Failed to verify internal request signature", ex);
      return false;
    }
  }

  private void recordBypass() {
    if (bypassCounter != null) {
      bypassCounter.increment();
    }
  }

  private void recordBurstUsage() {
    if (burstCounter != null) {
      burstCounter.increment();
    }
  }

  private void recordRateLimitDecision(ServerWebExchange exchange, LimitDefinition limit,
      RateLimitDecision decision) {
    if (tracingHelper != null) {
      tracingHelper.recordRateLimitDecision(exchange,
          decision.allowed(),
          decision.remaining(),
          decision.baseRemaining(),
          decision.burstRemaining(),
          limit.window(),
          decision.burstConsumed(),
          props.getKeyStrategy());
    }
    if (!decision.allowed()) {
      recordRejection(exchange);
    }
  }

  private void recordRejection(ServerWebExchange exchange) {
    if (meterRegistry == null) {
      return;
    }
    String strategy = trimToNull(props.getKeyStrategy());
    String tenant = trimToNull(exchange.getAttribute(GatewayRequestAttributes.TENANT_ID));
    meterRegistry.counter("gateway.ratelimit.rejections",
            "strategy", strategy != null ? strategy : "unknown",
            "tenantId", tenant != null ? tenant : "unknown")
        .increment();
  }

  private String rateKey(String key, String algorithm) {
    if (Objects.equals("sliding", algorithm)) {
      return "rl:sliding:" + key;
    }
    return "rl:" + key;
  }

  private String burstKey(String key) {
    return "rl:burst:" + key;
  }

  private LimitDefinition resolveLimit(ServerWebExchange exchange) {
    int defaultCapacity = Math.max(1, props.getCapacity());
    Duration defaultWindow = resolveBaseWindow();
    String rawTier = exchange.getAttribute(GatewayRequestAttributes.SUBSCRIPTION_TIER);
    String tier = trimToNull(rawTier);
    TierLimit tierLimit = (gatewayProps != null) ? gatewayProps.resolveTier(tier) : null;
    int capacity = (tierLimit != null) ? tierLimit.capacity() : defaultCapacity;
    Duration window = (tierLimit != null) ? tierLimit.window() : defaultWindow;
    String policy = (tierLimit != null && StringUtils.hasText(rawTier)) ? rawTier.trim() : "default";
    double multiplier = (gatewayProps != null) ? gatewayProps.getBurstMultiplier() : 1.0d;
    int burstCapacity = Math.max(capacity,
        (int) Math.ceil(capacity * Math.max(1.0d, multiplier)));
    return new LimitDefinition(capacity, window, policy, burstCapacity);
  }

  private Duration resolveBaseWindow() {
    Duration configured = props.getWindow();
    if (configured == null || configured.isZero() || configured.isNegative()) {
      return Duration.ofMinutes(1);
    }
    return configured;
  }

  private String resolveAlgorithm() {
    String algorithm = props.getAlgorithm();
    if (!StringUtils.hasText(algorithm)) {
      return "fixed";
    }
    algorithm = algorithm.trim().toLowerCase(Locale.ROOT);
    return Objects.equals(algorithm, "sliding") ? "sliding" : "fixed";
  }

  private Mono<String> resolveKey(ServerWebExchange exchange) {
    return keyResolver.resolve(exchange)
        .flatMap(value -> Mono.justOrEmpty(trimToNull(value)))
        .map(this::normalizeKey)
        .switchIfEmpty(Mono.fromCallable(() -> normalizeKey(resolveFallbackKey(exchange))))
        .onErrorResume(ex -> {
          LOGGER.warn("Key resolver {} failed, falling back to legacy resolution", keyResolver.getClass().getSimpleName(), ex);
          return Mono.fromCallable(() -> normalizeKey(resolveFallbackKey(exchange)));
        });
  }

  private String resolveFallbackKey(ServerWebExchange exchange) {
    return switch (props.getKeyStrategy()) {
      case "ip" -> {
        String forwarded = exchange.getRequest().getHeaders().getFirst(HeaderNames.CLIENT_IP);
        yield StringUtils.hasText(forwarded)
            ? forwarded
            : Objects.toString(exchange.getRequest().getRemoteAddress(), DEFAULT_FALLBACK_KEY);
      }
      case "user" -> {
        String userId = ContextManager.getUserId();
        yield StringUtils.hasText(userId) ? userId : DEFAULT_FALLBACK_KEY;
      }
      default -> {
        String tenant = ContextManager.Tenant.get();
        yield StringUtils.hasText(tenant) ? tenant : "public";
      }
    };
  }

  private String normalizeKey(String key) {
    if (!StringUtils.hasText(key)) {
      return DEFAULT_FALLBACK_KEY;
    }
    return key.trim().toLowerCase(Locale.ROOT);
  }

  private String trimToNull(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private Mono<Void> reject(ServerWebExchange exchange) {
    LOGGER.debug("Rate limit exceeded for request to {}", exchange.getRequest().getPath());
    var response = exchange.getResponse();
    response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
    BaseResponse<Void> body = BaseResponse.error("ERR_RATE_LIMIT", "Rate limit exceeded");
    byte[] payload;
    try {
      payload = (objectMapper != null)
          ? objectMapper.writeValueAsBytes(body)
          : body.toString().getBytes(StandardCharsets.UTF_8);
    } catch (JsonProcessingException e) {
      payload = body.toString().getBytes(StandardCharsets.UTF_8);
    }
    return response.writeWith(Mono.just(response.bufferFactory().wrap(payload)));
  }

  private static ObjectMapper resolveObjectMapper(ObjectProvider<ObjectMapper> primary,
      ObjectProvider<ObjectMapper> fallback) {
    ObjectMapper mapper = (primary != null) ? primary.getIfAvailable() : null;
    if (mapper != null) {
      return mapper;
    }
    return (fallback != null) ? fallback.getIfAvailable() : null;
  }

  private boolean parseBoolean(Object value) {
    return Objects.equals("1", String.valueOf(value));
  }

  private long parseLong(Object value, long defaultValue) {
    if (value == null) {
      return defaultValue;
    }
    try {
      return Long.parseLong(String.valueOf(value));
    } catch (NumberFormatException ex) {
      return defaultValue;
    }
  }

  private record LimitDefinition(int capacity, Duration window, String policy, int burstCapacity) {
  }

  private record RateLimitDecision(boolean allowed, long remaining, long resetEpochMillis,
      Duration window, boolean burstConsumed, long baseRemaining, long burstRemaining) {
  }
}
