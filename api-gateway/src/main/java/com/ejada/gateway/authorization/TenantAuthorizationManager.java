package com.ejada.gateway.authorization;

import com.ejada.common.constants.HeaderNames;
import com.ejada.gateway.config.GatewayRateLimitProperties;
import com.ejada.gateway.context.GatewayRequestAttributes;
import com.ejada.gateway.subscription.SubscriptionCacheService;
import com.ejada.gateway.subscription.SubscriptionRecord;
import com.ejada.starter_core.config.CoreAutoConfiguration;
import com.ejada.starter_security.Role;
import com.ejada.starter_security.SharedSecurityProps;
import com.ejada.starter_core.web.FilterSkipUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Reactive tenant authorization that performs multi-tenant isolation checks before requests reach
 * downstream services. The manager validates that the authenticated tenant matches the requested
 * tenant, ensures the tenant is active, enriches the exchange with subscription details and applies
 * tier-based rate limiting. Computed decisions are cached in Redis for fast re-use.
 */
public class TenantAuthorizationManager implements ReactiveAuthorizationManager<AuthorizationContext> {

  private static final Logger LOGGER = LoggerFactory.getLogger(TenantAuthorizationManager.class);

  private static final Pattern TENANT_SEGMENT_PATTERN =
      Pattern.compile("/tenants/([A-Za-z0-9_-]+)(?:/|$)", Pattern.CASE_INSENSITIVE);
  private static final Pattern TIER_PATTERN =
      Pattern.compile("(?i)tier[:/_-]?([a-z0-9]+)");

  private static final String[] DEFAULT_PERMIT_ALL_PATTERNS = new String[]{
      "/auth/**",
      "/api/auth/**",
      "/api/auth/superadmin/**",
      "/api/*/auth/**",
      "/api/v1/auth/**",
      "/api/v1/auth/admin/**",
      "/api/v1/superadmin/**",
      "/api/auth/**",
      "/api/v1/auth/**",
      "/api/v1/superadmin/**",
      "/auth/**"
  };

  private static final Duration CACHE_TTL = Duration.ofMinutes(5);
  private static final String DEFAULT_TIER = "free";
  private static final Set<String> SUSPENDED_STATUSES = Set.of("INACTIVE", "SUSPENDED");

  private final CoreAutoConfiguration.CoreProps coreProps;
  private final String[] bypassPatterns;
  private final String[] permitAllPatterns;
  private final ReactiveStringRedisTemplate redisTemplate;
  private final SubscriptionCacheService subscriptionCacheService;
  private final GatewayRateLimitProperties rateLimitProperties;
  private final ObjectMapper objectMapper;

  public TenantAuthorizationManager(
      CoreAutoConfiguration.CoreProps coreProps,
      SharedSecurityProps securityProps,
      ReactiveStringRedisTemplate redisTemplate,
      SubscriptionCacheService subscriptionCacheService,
      GatewayRateLimitProperties rateLimitProperties,
      ObjectMapper objectMapper) {
    this.coreProps = Objects.requireNonNull(coreProps, "coreProps");
    this.bypassPatterns = mergeBypassPatterns(coreProps, securityProps);
    this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate");
    this.subscriptionCacheService =
        Objects.requireNonNull(subscriptionCacheService, "subscriptionCacheService");
    this.rateLimitProperties = Objects.requireNonNull(rateLimitProperties, "rateLimitProperties");
    this.objectMapper = (objectMapper != null) ? objectMapper : new ObjectMapper();
    Stream<String> configuredPermitAll = Optional.ofNullable(securityProps)
        .map(SharedSecurityProps::getResourceServer)
        .map(SharedSecurityProps.ResourceServer::getPermitAll)
        .map(Arrays::stream)
        .orElseGet(Stream::empty);
    this.permitAllPatterns = Stream.concat(Arrays.stream(DEFAULT_PERMIT_ALL_PATTERNS), configuredPermitAll)
        .filter(StringUtils::hasText)
        .map(String::trim)
        .distinct()
        .toArray(String[]::new);
  }

  @Override
  public Mono<AuthorizationDecision> check(
      Mono<Authentication> authentication, AuthorizationContext context) {
    ServerWebExchange exchange = context.getExchange();
    String path = exchange.getRequest().getPath().pathWithinApplication().value();
    if (FilterSkipUtils.shouldSkip(path, bypassPatterns)
        || FilterSkipUtils.shouldSkip(path, permitAllPatterns)) {
      return Mono.just(new AuthorizationDecision(true));
    }
    return authentication
        .filter(Authentication::isAuthenticated)
        .flatMap(auth -> evaluateAuthorization(auth, exchange))
        .defaultIfEmpty(new AuthorizationDecision(false));
  }

  private static String[] mergeBypassPatterns(CoreAutoConfiguration.CoreProps coreProps,
      SharedSecurityProps securityProps) {
    String[] tenantSkips = FilterSkipUtils.copyOrDefault(coreProps.getTenant().getSkipPatterns());
    Stream<String> configuredPermitAll = Optional.ofNullable(securityProps)
        .map(SharedSecurityProps::getResourceServer)
        .map(SharedSecurityProps.ResourceServer::getPermitAll)
        .map(Arrays::stream)
        .orElseGet(Stream::empty);
    Stream<String> permitAllStream =
        Stream.concat(Arrays.stream(DEFAULT_PERMIT_ALL_PATTERNS), configuredPermitAll);
    return Stream.concat(Arrays.stream(tenantSkips), permitAllStream)
        .filter(Objects::nonNull)
        .map(String::trim)
        .filter(value -> !value.isEmpty())
        .distinct()
        .toArray(String[]::new);
  }

  private Mono<AuthorizationDecision> evaluateAuthorization(Authentication auth, ServerWebExchange exchange) {
    String resolvedTenant = trimToNull(exchange.getAttribute(GatewayRequestAttributes.TENANT_ID));
    String headerTenant = trimToNull(exchange.getRequest().getHeaders().getFirst(coreProps.getTenant().getHeaderName()));
    String pathTenant = extractTenantFromPath(exchange);
    String jwtTenant = extractTenantFromJwt(auth);
    boolean superAdmin = hasSuperAdminAuthority(auth.getAuthorities());

    if (superAdmin
        && !StringUtils.hasText(resolvedTenant)
        && !StringUtils.hasText(headerTenant)
        && !StringUtils.hasText(pathTenant)
        && !StringUtils.hasText(jwtTenant)) {
      exchange.getResponse().getHeaders().set("X-Tenant-Verified", "super-admin");
      return Mono.just(new AuthorizationDecision(true));
    }

    if (!superAdmin && StringUtils.hasText(jwtTenant)) {
      if (!matches(jwtTenant, resolvedTenant) || !matches(jwtTenant, headerTenant) || !matches(jwtTenant, pathTenant)) {
        LOGGER.debug("JWT tenant {} does not match request tenant (header={}, path={}, resolved={})",
            jwtTenant, headerTenant, pathTenant, resolvedTenant);
        exchange.getResponse().getHeaders().set("X-Tenant-Verified", "false");
        return Mono.just(new AuthorizationDecision(false));
      }
    }

    if (!superAdmin) {
      if (StringUtils.hasText(headerTenant) && StringUtils.hasText(pathTenant) && !equalsIgnoreCase(headerTenant, pathTenant)) {
        LOGGER.debug("Header tenant {} does not match path tenant {}", headerTenant, pathTenant);
        exchange.getResponse().getHeaders().set("X-Tenant-Verified", "false");
        return Mono.just(new AuthorizationDecision(false));
      }
      if (StringUtils.hasText(resolvedTenant) && StringUtils.hasText(pathTenant)
          && !equalsIgnoreCase(resolvedTenant, pathTenant)) {
        LOGGER.debug("Resolved tenant {} does not match path tenant {}", resolvedTenant, pathTenant);
        exchange.getResponse().getHeaders().set("X-Tenant-Verified", "false");
        return Mono.just(new AuthorizationDecision(false));
      }
    }

    String targetTenant = firstNonNull(resolvedTenant, headerTenant, pathTenant, jwtTenant);

    if (superAdmin && !StringUtils.hasText(targetTenant)) {
      exchange.getResponse().getHeaders().set("X-Tenant-Verified", "super-admin");
      return Mono.just(new AuthorizationDecision(true));
    }

    if (!StringUtils.hasText(targetTenant)) {
      LOGGER.debug("Unable to resolve tenant for request {} {}", exchange.getRequest().getMethod(),
          exchange.getRequest().getURI());
      exchange.getResponse().getHeaders().set("X-Tenant-Verified", "false");
      return Mono.just(new AuthorizationDecision(false));
    }

    String finalTenant = targetTenant;
    return resolveTenantAccess(finalTenant)
        .flatMap(access -> enforceTenantPolicy(access, finalTenant, exchange, superAdmin))
        .doOnNext(decision -> {
          if (decision.isGranted()) {
            exchange.getResponse().getHeaders().set("X-Tenant-Verified", finalTenant);
          } else {
            exchange.getResponse().getHeaders().set("X-Tenant-Verified", "false");
          }
        })
        .defaultIfEmpty(new AuthorizationDecision(false));
  }

  private Mono<AuthorizationDecision> enforceTenantPolicy(
      TenantAccess access, String tenantId, ServerWebExchange exchange, boolean superAdmin) {
    if (access == null) {
      return Mono.just(new AuthorizationDecision(false));
    }
    if (!access.active() || access.isSuspended()) {
      LOGGER.debug("Blocking tenant {} due to inactive status {}", tenantId, access.status());
      return Mono.just(new AuthorizationDecision(false));
    }
    if (StringUtils.hasText(access.tier())) {
      exchange.getAttributes().put(GatewayRequestAttributes.SUBSCRIPTION_TIER, access.tier());
    }
    exchange.getAttributes().putIfAbsent(HeaderNames.X_TENANT_ID, tenantId);
    return applyTierRateLimit(tenantId, access.tier(), superAdmin)
        .map(allowed -> new AuthorizationDecision(allowed));
  }

  private Mono<Boolean> applyTierRateLimit(String tenantId, String tier, boolean superAdmin) {
    String normalizedTier = StringUtils.hasText(tier) ? tier.trim().toLowerCase(Locale.ROOT) : DEFAULT_TIER;
    GatewayRateLimitProperties.TierLimit tierLimit = rateLimitProperties.resolveTier(normalizedTier);
    if (tierLimit == null) {
      return Mono.just(true);
    }
    if (superAdmin) {
      return Mono.just(true);
    }
    String key = "gateway:tenant:rate:" + normalizedTier + ':' + tenantId.toLowerCase(Locale.ROOT);
    Duration window = tierLimit.window();
    int capacity = Math.max(1, tierLimit.capacity());
    return redisTemplate.opsForValue().increment(key)
        .flatMap(count -> {
          Mono<Boolean> expiryMono = Mono.just(true);
          if (count != null && count == 1L && window != null && !window.isZero() && !window.isNegative()) {
            expiryMono = redisTemplate.expire(key, window).onErrorReturn(true);
          }
          return expiryMono.thenReturn(count != null && count <= capacity);
        })
        .onErrorResume(ex -> {
          LOGGER.warn("Failed to enforce rate limit for tenant {} tier {}", tenantId, normalizedTier, ex);
          return Mono.just(true);
        });
  }

  private Mono<TenantAccess> resolveTenantAccess(String tenantId) {
    String cacheKey = permissionCacheKey(tenantId);
    return redisTemplate.opsForValue().get(cacheKey)
        .flatMap(json -> decode(json).map(Mono::just).orElseGet(Mono::empty))
        .switchIfEmpty(fetchAndCacheTenantAccess(tenantId));
  }

  private Mono<TenantAccess> fetchAndCacheTenantAccess(String tenantId) {
    return subscriptionCacheService.getCached(tenantId)
        .flatMap(optional -> optional
            .map(record -> Mono.just(buildAccess(record)))
            .orElseGet(() -> subscriptionCacheService.fetchAndCache(tenantId).map(this::buildAccess)))
        .flatMap(access -> cacheTenantAccess(tenantId, access).thenReturn(access))
        .switchIfEmpty(Mono.defer(() -> Mono.just(buildAccess(null))));
  }

  private TenantAccess buildAccess(SubscriptionRecord record) {
    if (record == null) {
      return new TenantAccess(false, "UNKNOWN", DEFAULT_TIER, Collections.emptySet(), Instant.now());
    }
    boolean active = record.isActive();
    String status = StringUtils.hasText(record.status()) ? record.status().trim().toUpperCase(Locale.ROOT) : "UNKNOWN";
    String tier = deriveTier(record);
    Set<String> permissions = Optional.ofNullable(record.enabledFeatures()).orElseGet(Collections::emptySet);
    return new TenantAccess(active, status, tier, permissions, record.fetchedAt());
  }

  private Mono<Boolean> cacheTenantAccess(String tenantId, TenantAccess access) {
    try {
      String json = objectMapper.writeValueAsString(access);
      return redisTemplate.opsForValue().set(permissionCacheKey(tenantId), json, CACHE_TTL)
          .onErrorReturn(false)
          .thenReturn(true);
    } catch (JsonProcessingException e) {
      LOGGER.debug("Failed to serialise tenant access for {}", tenantId, e);
      return Mono.just(false);
    }
  }

  private Optional<TenantAccess> decode(String json) {
    try {
      return Optional.ofNullable(objectMapper.readValue(json, TenantAccess.class));
    } catch (Exception ex) {
      LOGGER.debug("Failed to decode cached tenant access", ex);
      return Optional.empty();
    }
  }

  private String permissionCacheKey(String tenantId) {
    String safeTenant = StringUtils.hasText(tenantId) ? tenantId.trim().toLowerCase(Locale.ROOT) : "unknown";
    return "gateway:tenant:permissions:" + safeTenant;
  }

  private String deriveTier(SubscriptionRecord record) {
    if (record == null) {
      return DEFAULT_TIER;
    }
    for (String feature : Optional.ofNullable(record.enabledFeatures()).orElse(Set.of())) {
      String tier = extractTier(feature);
      if (tier != null) {
        return tier;
      }
    }
    for (String key : Optional.ofNullable(record.allocations()).map(map -> map.keySet()).orElse(Set.of())) {
      String tier = extractTier(key);
      if (tier != null) {
        return tier;
      }
    }
    return DEFAULT_TIER;
  }

  private String extractTier(String candidate) {
    if (!StringUtils.hasText(candidate)) {
      return null;
    }
    Matcher matcher = TIER_PATTERN.matcher(candidate.trim());
    if (matcher.matches() || matcher.find()) {
      return matcher.group(1).toLowerCase(Locale.ROOT);
    }
    return null;
  }

  private boolean matches(String expected, String candidate) {
    if (!StringUtils.hasText(candidate)) {
      return true;
    }
    return equalsIgnoreCase(expected, candidate);
  }

  private boolean equalsIgnoreCase(String a, String b) {
    if (!StringUtils.hasText(a) || !StringUtils.hasText(b)) {
      return false;
    }
    return a.trim().equalsIgnoreCase(b.trim());
  }

  private String extractTenantFromJwt(Authentication authentication) {
    if (!(authentication instanceof JwtAuthenticationToken jwtAuthenticationToken)) {
      return null;
    }
    for (String claim : coreProps.getTenant().getJwtClaimNames()) {
      Object value = jwtAuthenticationToken.getToken().getClaims().get(claim);
      if (value != null) {
        String tenant = trimToNull(Objects.toString(value, null));
        if (tenant != null) {
          return tenant;
        }
      }
    }
    return null;
  }

  private String extractTenantFromPath(ServerWebExchange exchange) {
    String path = exchange.getRequest().getPath().pathWithinApplication().value();
    Matcher matcher = TENANT_SEGMENT_PATTERN.matcher(path);
    if (matcher.find()) {
      return trimToNull(matcher.group(1));
    }
    return null;
  }

  private boolean hasSuperAdminAuthority(Iterable<? extends GrantedAuthority> authorities) {
    if (authorities == null) {
      return false;
    }
    for (GrantedAuthority authority : authorities) {
      if (authority == null || authority.getAuthority() == null) {
        continue;
      }
      String value = authority.getAuthority();
      if (isSuperAdminAuthority(value)) {
        return true;
      }
    }
    return false;
  }

  private boolean isSuperAdminAuthority(String value) {
    if (!StringUtils.hasText(value)) {
      return false;
    }
    String normalized = value.trim();
    return "SUPER_ADMIN".equalsIgnoreCase(normalized)
        || "ROLE_SUPER_ADMIN".equalsIgnoreCase(normalized)
        || Role.EJADA_OFFICER.getAuthority().equalsIgnoreCase(normalized)
        || Role.EJADA_OFFICER.name().equalsIgnoreCase(normalized);
  }

  private static String firstNonNull(String... values) {
    if (values == null) {
      return null;
    }
    for (String value : values) {
      if (StringUtils.hasText(value)) {
        return value;
      }
    }
    return null;
  }

  private static String trimToNull(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private record TenantAccess(
      boolean active,
      String status,
      String tier,
      Set<String> permissions,
      Instant cachedAt) {

    boolean isSuspended() {
      if (!StringUtils.hasText(status)) {
        return false;
      }
      return SUSPENDED_STATUSES.contains(status.trim().toUpperCase(Locale.ROOT));
    }
  }
}
