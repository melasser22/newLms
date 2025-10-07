package com.ejada.gateway.security;

import com.ejada.common.constants.HeaderNames;
import com.ejada.common.dto.BaseResponse;
import com.ejada.gateway.config.GatewayRoutesProperties;
import com.ejada.gateway.config.GatewayRoutesProperties.ServiceRoute;
import com.ejada.gateway.config.GatewaySecurityProperties;
import com.ejada.gateway.config.GatewaySecurityProperties.EncryptionAlgorithm;
import com.ejada.gateway.context.GatewayRequestAttributes;
import com.ejada.gateway.security.GatewaySecurityMetrics;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Validates X-API-Key headers against Redis backed records and populates tenant context.
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class ApiKeyAuthenticationFilter implements WebFilter, Ordered {

  private static final Logger LOGGER = LoggerFactory.getLogger(ApiKeyAuthenticationFilter.class);
  private static final int GCM_TAG_LENGTH = 128;
  private static final int GCM_IV_LENGTH = 12;

  private final ReactiveStringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;
  private final GatewaySecurityProperties properties;
  private final GatewaySecurityMetrics metrics;
  private final GatewayRoutesProperties routesProperties;
  private final PathMatcher pathMatcher = new AntPathMatcher();
  private final SecureRandom secureRandom = new SecureRandom();

  private volatile SecretKey cachedSecretKey;
  private volatile String cachedKeyValue;

  public ApiKeyAuthenticationFilter(
      ReactiveStringRedisTemplate redisTemplate,
      GatewaySecurityMetrics metrics,
      GatewaySecurityProperties properties,
      GatewayRoutesProperties routesProperties,
      @Qualifier("jacksonObjectMapper") ObjectProvider<ObjectMapper> primaryObjectMapper,
      ObjectProvider<ObjectMapper> fallbackObjectMapper) {
    this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate");
    this.properties = Objects.requireNonNull(properties, "properties");
    this.metrics = Objects.requireNonNull(metrics, "metrics");
    this.routesProperties = Objects.requireNonNull(routesProperties, "routesProperties");
    ObjectMapper mapper = (primaryObjectMapper != null) ? primaryObjectMapper.getIfAvailable() : null;
    if (mapper == null) {
      mapper = (fallbackObjectMapper != null) ? fallbackObjectMapper.getIfAvailable() : null;
    }
    this.objectMapper = Objects.requireNonNull(mapper, "objectMapper");
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    if (!properties.getApiKey().isEnabled()) {
      return chain.filter(exchange);
    }

    String apiKey = trimToNull(exchange.getRequest().getHeaders().getFirst(HeaderNames.API_KEY));
    if (!StringUtils.hasText(apiKey)) {
      return chain.filter(exchange);
    }

    String redisKey = properties.getApiKey().redisKey(apiKey);
    return redisTemplate.opsForValue().get(redisKey)
        .flatMap(payload -> decodeRecord(payload)
            .onErrorResume(ex -> {
              LOGGER.warn("Failed to decode API key payload for key {}", redisKey, ex);
              metrics.incrementBlocked("api_key_decode", null);
              return reject(exchange, HttpStatus.UNAUTHORIZED, "ERR_API_KEY_INVALID", "Invalid API key")
                  .then(Mono.<DecodedApiKeyRecord>empty());
            }))
        .switchIfEmpty(Mono.defer(() -> {
          LOGGER.debug("API key not found for redis key {}", redisKey);
          metrics.incrementBlocked("api_key", null);
          return reject(exchange, HttpStatus.UNAUTHORIZED, "ERR_API_KEY_INVALID", "Invalid API key")
              .then(Mono.<DecodedApiKeyRecord>empty());
        }))
        .flatMap(decoded -> validateAndAuthenticate(decoded, apiKey, redisKey, exchange, chain));
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 1;
  }

  private Mono<DecodedApiKeyRecord> decodeRecord(String raw) {
    return Mono.fromCallable(() -> {
      JsonNode tree = objectMapper.readTree(raw);
      if (tree.hasNonNull("ciphertext")) {
        EncryptedPayload payload = objectMapper.treeToValue(tree, EncryptedPayload.class);
        ApiKeyRecord record = decrypt(payload);
        return DecodedApiKeyRecord.encrypted(record, payload.keyId());
      }
      PlainStoredApiKeyRecord stored = objectMapper.treeToValue(tree, PlainStoredApiKeyRecord.class);
      return DecodedApiKeyRecord.plain(stored.toRecord());
    });
  }

  private Mono<Void> validateAndAuthenticate(DecodedApiKeyRecord decoded, String apiKey, String redisKey,
      ServerWebExchange exchange, WebFilterChain chain) {
    ApiKeyRecord record = decoded.record();
    Instant now = Instant.now();
    if (!StringUtils.hasText(record.getTenantId())) {
      metrics.incrementBlocked("api_key", null);
      return reject(exchange, HttpStatus.UNAUTHORIZED, "ERR_API_KEY_INVALID", "API key is not bound to a tenant");
    }
    if (record.getExpiresAt() != null && record.getExpiresAt().isBefore(now)) {
      metrics.incrementBlocked("api_key_expired", record.getTenantId());
      return reject(exchange, HttpStatus.UNAUTHORIZED, "ERR_API_KEY_EXPIRED", "API key has expired");
    }

    GatewaySecurityProperties.ApiKey.Rotation rotation = properties.getApiKey().getRotation();
    if (rotation.isEnabled()) {
      Instant rotatedAt = record.getRotatedAt();
      if (rotatedAt == null) {
        metrics.incrementBlocked("api_key_rotation_missing", record.getTenantId());
        return reject(exchange, HttpStatus.UNAUTHORIZED, "ERR_API_KEY_ROTATION_REQUIRED", "API key rotation metadata missing");
      }
      Duration maxAge = Duration.ofDays(Math.max(1, rotation.getMaxAgeDays()));
      if (rotatedAt.plus(maxAge).isBefore(now)) {
        metrics.incrementBlocked("api_key_rotation_expired", record.getTenantId());
        return reject(exchange, HttpStatus.UNAUTHORIZED, "ERR_API_KEY_ROTATED", "API key rotation window exceeded");
      }
    }

    ScopeRequirements requirements = resolveScopeRequirements(exchange);
    if (!requirements.scopes().isEmpty()) {
      Set<String> granted = normaliseScopes(record.getScopes());
      Set<String> required = normaliseScopes(requirements.scopes());
      boolean requireExact = properties.getApiKey().getScopeEnforcement().isRequireExactMatch();
      boolean authorised = requireExact ? granted.equals(required) : granted.containsAll(required);
      if (!authorised) {
        metrics.incrementBlocked("api_key_scope", record.getTenantId());
        String message = requireExact
            ? "API key scopes do not exactly match route requirements"
            : "API key missing required scopes";
        return reject(exchange, HttpStatus.FORBIDDEN, "ERR_API_KEY_SCOPE", message);
      }
    }

    Collection<GrantedAuthority> authorities = authorities(record.getScopes());
    ApiKeyAuthenticationToken authentication = new ApiKeyAuthenticationToken(apiKey, record.getTenantId(), authorities);

    metrics.incrementApiKeyValidated(record.getTenantId());

    ServerHttpRequest mutatedRequest = mutateRequest(exchange.getRequest(), record.getTenantId());
    ServerWebExchange mutatedExchange = exchange.mutate()
        .request(mutatedRequest)
        .build();
    mutatedExchange.getAttributes().put(GatewayRequestAttributes.TENANT_ID, record.getTenantId());
    mutatedExchange.getAttributes().put(HeaderNames.X_TENANT_ID, record.getTenantId());

    Mono<Void> audit = auditUsage(decoded, redisKey, record, exchange)
        .onErrorResume(ex -> {
          LOGGER.warn("Failed to audit API key usage for tenant {}", record.getTenantId(), ex);
          return Mono.empty();
        });

    return audit.then(chain.filter(mutatedExchange)
        .contextWrite(ctx -> ctx
            .put(GatewayRequestAttributes.TENANT_ID, record.getTenantId())
            .put(HeaderNames.X_TENANT_ID, record.getTenantId())
            .put(SecurityContext.class, new SecurityContextImpl(authentication)))
        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication)));
  }

  private Mono<Void> auditUsage(DecodedApiKeyRecord decoded, String redisKey, ApiKeyRecord record,
      ServerWebExchange exchange) {
    GatewaySecurityProperties.ApiKey.Audit audit = properties.getApiKey().getAudit();
    if (!audit.isLogUsage() && !audit.isTrackLastUsed()) {
      return Mono.empty();
    }
    Instant now = Instant.now();
    ScopeRequirements requirements = resolveScopeRequirements(exchange);
    if (audit.isLogUsage()) {
      LOGGER.info("API key usage: tenant={}, path={}, routes={}, keyId={}",
          record.getTenantId(), exchange.getRequest().getPath(),
          String.join(",", requirements.routeIds()), decoded.keyId().orElse("n/a"));
    }
    if (!audit.isTrackLastUsed()) {
      return Mono.empty();
    }
    Instant previous = record.getLastUsedAt();
    if (previous != null && !previous.isBefore(now.minusSeconds(1))) {
      return Mono.empty();
    }
    record.setLastUsedAt(now);
    return Mono.fromCallable(() -> serializeRecord(record, decoded.encrypted()))
        .flatMap(serialized -> {
          if (!StringUtils.hasText(serialized)) {
            return Mono.<Void>empty();
          }
          return redisTemplate.opsForValue().set(redisKey, serialized).then();
        });
  }

  private ServerHttpRequest mutateRequest(ServerHttpRequest request, String tenantId) {
    if (StringUtils.hasText(request.getHeaders().getFirst(HeaderNames.X_TENANT_ID))) {
      return request;
    }
    return request.mutate()
        .header(HeaderNames.X_TENANT_ID, tenantId)
        .build();
  }

  private Collection<GrantedAuthority> authorities(Set<String> scopes) {
    if (scopes == null || scopes.isEmpty()) {
      return List.of();
    }
    return scopes.stream()
        .filter(StringUtils::hasText)
        .map(scope -> scope.startsWith("SCOPE_") ? scope : "SCOPE_" + scope)
        .map(SimpleGrantedAuthority::new)
        .collect(Collectors.toUnmodifiableList());
  }

  private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, String code, String message) {
    var response = exchange.getResponse();
    if (response.isCommitted()) {
      LOGGER.debug("Response already committed, skipping API key rejection for {}", exchange.getRequest().getPath());
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

  private ScopeRequirements resolveScopeRequirements(ServerWebExchange exchange) {
    GatewaySecurityProperties.ApiKey.ScopeEnforcement enforcement = properties.getApiKey().getScopeEnforcement();
    if (!enforcement.isEnabled()) {
      return ScopeRequirements.empty();
    }
    String path = exchange.getRequest().getURI().getPath();
    HttpMethod method = exchange.getRequest().getMethod();
    Set<String> scopes = new LinkedHashSet<>();
    Set<String> routeIds = new LinkedHashSet<>();
    for (ServiceRoute route : routesProperties.getRoutes().values()) {
      if (route == null || route.getRequiredScopes().isEmpty()) {
        continue;
      }
      if (!methodMatches(route, method)) {
        continue;
      }
      if (!pathMatches(route, path)) {
        continue;
      }
      scopes.addAll(route.getRequiredScopes());
      if (StringUtils.hasText(route.getId())) {
        routeIds.add(route.getId());
      }
    }
    return scopes.isEmpty() ? ScopeRequirements.empty() : new ScopeRequirements(scopes, routeIds);
  }

  private boolean methodMatches(ServiceRoute route, HttpMethod method) {
    if (route.getMethods() == null || route.getMethods().isEmpty()) {
      return true;
    }
    if (method == null) {
      return false;
    }
    String current = method.name().toUpperCase(Locale.ROOT);
    return route.getMethods().stream()
        .filter(StringUtils::hasText)
        .map(value -> value.trim().toUpperCase(Locale.ROOT))
        .anyMatch(current::equals);
  }

  private boolean pathMatches(ServiceRoute route, String path) {
    if (route.getPaths() == null || route.getPaths().isEmpty()) {
      return false;
    }
    for (String pattern : route.getPaths()) {
      if (StringUtils.hasText(pattern) && pathMatcher.match(pattern, path)) {
        return true;
      }
    }
    return false;
  }

  private Set<String> normaliseScopes(Collection<String> scopes) {
    if (scopes == null || scopes.isEmpty()) {
      return Set.of();
    }
    return scopes.stream()
        .filter(StringUtils::hasText)
        .map(String::trim)
        .map(scope -> scope.startsWith("SCOPE_") ? scope.substring("SCOPE_".length()) : scope)
        .map(value -> value.toLowerCase(Locale.ROOT))
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private ApiKeyRecord decrypt(EncryptedPayload payload) throws GeneralSecurityException, IOException {
    if (!StringUtils.hasText(payload.ciphertext()) || !StringUtils.hasText(payload.iv())) {
      throw new IllegalArgumentException("Encrypted payload missing ciphertext or IV");
    }
    EncryptionAlgorithm algorithm = Optional.ofNullable(payload.algorithm())
        .map(EncryptionAlgorithm::from)
        .orElse(properties.getApiKey().getEncryption().getAlgorithm());
    if (algorithm != EncryptionAlgorithm.AES_256_GCM) {
      throw new IllegalStateException("Unsupported API key encryption algorithm: " + payload.algorithm());
    }
    SecretKey key = resolveEncryptionKey();
    byte[] iv = Base64.getDecoder().decode(payload.iv());
    byte[] ciphertext = Base64.getDecoder().decode(payload.ciphertext());
    Cipher cipher = Cipher.getInstance(algorithm.getTransformation());
    cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
    byte[] plaintext = cipher.doFinal(ciphertext);
    PlainStoredApiKeyRecord stored = objectMapper.readValue(plaintext, PlainStoredApiKeyRecord.class);
    return stored.toRecord();
  }

  private String serializeRecord(ApiKeyRecord record, boolean wasEncrypted)
      throws GeneralSecurityException, JsonProcessingException {
    GatewaySecurityProperties.ApiKey.Encryption encryption = properties.getApiKey().getEncryption();
    boolean shouldEncrypt = encryption.isEnabled() && resolveEncryptionKeyOptional().isPresent();
    if (wasEncrypted) {
      shouldEncrypt = true;
    }
    if (shouldEncrypt) {
      EncryptionAlgorithm algorithm = encryption.getAlgorithm();
      EncryptedPayload payload = encrypt(record, algorithm, encryption.getKeyId());
      return objectMapper.writeValueAsString(payload);
    }
    PlainStoredApiKeyRecord stored = PlainStoredApiKeyRecord.from(record);
    return objectMapper.writeValueAsString(stored);
  }

  private EncryptedPayload encrypt(ApiKeyRecord record, EncryptionAlgorithm algorithm, String keyId)
      throws GeneralSecurityException, JsonProcessingException {
    SecretKey key = resolveEncryptionKey();
    byte[] iv = new byte[GCM_IV_LENGTH];
    secureRandom.nextBytes(iv);
    Cipher cipher = Cipher.getInstance(algorithm.getTransformation());
    cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
    byte[] plaintext = objectMapper.writeValueAsBytes(PlainStoredApiKeyRecord.from(record));
    byte[] ciphertext = cipher.doFinal(plaintext);
    return new EncryptedPayload(
        Base64.getEncoder().encodeToString(ciphertext),
        Base64.getEncoder().encodeToString(iv),
        algorithm.getId(),
        keyId);
  }

  private SecretKey resolveEncryptionKey() {
    return resolveEncryptionKeyOptional()
        .orElseThrow(() -> new IllegalStateException("API key encryption key is not configured"));
  }

  private Optional<SecretKey> resolveEncryptionKeyOptional() {
    GatewaySecurityProperties.ApiKey.Encryption encryption = properties.getApiKey().getEncryption();
    String keyValue = encryption.getKeyValue();
    if (!StringUtils.hasText(keyValue)) {
      return Optional.empty();
    }
    SecretKey cached = this.cachedSecretKey;
    if (cached != null && keyValue.equals(this.cachedKeyValue)) {
      return Optional.of(cached);
    }
    byte[] decoded = Base64.getDecoder().decode(keyValue);
    if (!(decoded.length == 16 || decoded.length == 24 || decoded.length == 32)) {
      throw new IllegalStateException("API key encryption key must be 128, 192 or 256 bits");
    }
    SecretKey secretKey = new SecretKeySpec(decoded, "AES");
    this.cachedSecretKey = secretKey;
    this.cachedKeyValue = keyValue;
    return Optional.of(secretKey);
  }

  private static String trimToNull(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private record DecodedApiKeyRecord(ApiKeyRecord record, boolean encrypted, Optional<String> keyId) {

    static DecodedApiKeyRecord plain(ApiKeyRecord record) {
      return new DecodedApiKeyRecord(record, false, Optional.empty());
    }

    static DecodedApiKeyRecord encrypted(ApiKeyRecord record, String keyId) {
      return new DecodedApiKeyRecord(record, true, Optional.ofNullable(keyId));
    }
  }

  private record EncryptedPayload(String ciphertext, String iv, String algorithm, String keyId) {
  }

  private static final class PlainStoredApiKeyRecord {

    private String tenantId;
    private Set<String> scopes;
    private Instant expiresAt;
    private Instant rotatedAt;
    private Instant lastUsedAt;

    ApiKeyRecord toRecord() {
      ApiKeyRecord record = new ApiKeyRecord();
      record.setTenantId(tenantId);
      record.setScopes(scopes);
      record.setExpiresAt(expiresAt);
      record.setRotatedAt(rotatedAt);
      record.setLastUsedAt(lastUsedAt);
      return record;
    }

    static PlainStoredApiKeyRecord from(ApiKeyRecord record) {
      PlainStoredApiKeyRecord stored = new PlainStoredApiKeyRecord();
      stored.tenantId = record.getTenantId();
      stored.scopes = (record.getScopes() == null) ? Set.of() : new LinkedHashSet<>(record.getScopes());
      stored.expiresAt = record.getExpiresAt();
      stored.rotatedAt = record.getRotatedAt();
      stored.lastUsedAt = record.getLastUsedAt();
      return stored;
    }
  }

  private static final class ApiKeyRecord {

    private String tenantId;
    private Set<String> scopes = Set.of();
    private Instant expiresAt;
    private Instant rotatedAt;
    private Instant lastUsedAt;

    String getTenantId() {
      return tenantId;
    }

    void setTenantId(String tenantId) {
      this.tenantId = tenantId;
    }

    Set<String> getScopes() {
      return (scopes == null) ? Set.of() : scopes;
    }

    void setScopes(Collection<String> scopes) {
      if (scopes == null || scopes.isEmpty()) {
        this.scopes = Set.of();
        return;
      }
      this.scopes = scopes.stream()
          .filter(Objects::nonNull)
          .map(String::trim)
          .filter(StringUtils::hasText)
          .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    Instant getExpiresAt() {
      return expiresAt;
    }

    void setExpiresAt(Instant expiresAt) {
      this.expiresAt = expiresAt;
    }

    Instant getRotatedAt() {
      return rotatedAt;
    }

    void setRotatedAt(Instant rotatedAt) {
      this.rotatedAt = rotatedAt;
    }

    Instant getLastUsedAt() {
      return lastUsedAt;
    }

    void setLastUsedAt(Instant lastUsedAt) {
      this.lastUsedAt = lastUsedAt;
    }
  }

  private record ScopeRequirements(Set<String> scopes, Set<String> routeIds) {
    static ScopeRequirements empty() {
      return new ScopeRequirements(Set.of(), Set.of());
    }
  }
}
