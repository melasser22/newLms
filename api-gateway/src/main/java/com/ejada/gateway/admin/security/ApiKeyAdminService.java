package com.ejada.gateway.admin.security;

import com.ejada.gateway.config.GatewaySecurityProperties;
import com.ejada.gateway.security.apikey.ApiKeyCodec;
import com.ejada.gateway.security.apikey.ApiKeyCodec.DecodedApiKeyRecord;
import com.ejada.gateway.security.apikey.ApiKeyRecord;
import java.time.Instant;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ApiKeyAdminService {

  private final ReactiveStringRedisTemplate redisTemplate;
  private final GatewaySecurityProperties properties;
  private final ApiKeyCodec apiKeyCodec;

  public ApiKeyAdminService(ReactiveStringRedisTemplate redisTemplate,
      GatewaySecurityProperties properties,
      ApiKeyCodec apiKeyCodec) {
    this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate");
    this.properties = Objects.requireNonNull(properties, "properties");
    this.apiKeyCodec = Objects.requireNonNull(apiKeyCodec, "apiKeyCodec");
  }

  public Mono<ApiKeyCreatedResponse> create(String tenantId, CreateApiKeyRequest request) {
    CreateApiKeyRequest payload = request != null ? request : new CreateApiKeyRequest(Set.of(), null, null, null);
    String apiKey = apiKeyCodec.generateKey();
    ApiKeyRecord record = new ApiKeyRecord();
    record.setTenantId(tenantId);
    record.setScopes(payload.scopes());
    record.setLabel(payload.label());
    record.setRateLimitPerMinute(payload.rateLimitPerMinute());
    record.setExpiresAt(payload.expiresAt());
    Instant now = Instant.now();
    record.setCreatedAt(now);
    record.setRotatedAt(now);

    return store(apiKey, record)
        .then(addToIndex(tenantId, apiKey))
        .thenReturn(new ApiKeyCreatedResponse(apiKey, ApiKeyView.from(record, apiKey)));
  }

  public Flux<ApiKeyView> list(String tenantId) {
    String indexKey = properties.getApiKey().tenantIndexKey(tenantId);
    return redisTemplate.opsForSet().members(indexKey)
        .filter(StringUtils::hasText)
        .map(String::trim)
        .flatMap(apiKey -> fetchRecord(apiKey)
            .map(decoded -> ApiKeyView.from(decoded.record(), apiKey))
            .switchIfEmpty(Mono.defer(() -> removeDanglingIndex(indexKey, apiKey).then(Mono.empty()))));
  }

  public Mono<Void> delete(String tenantId, String apiKey) {
    String redisKey = properties.getApiKey().redisKey(apiKey);
    String indexKey = properties.getApiKey().tenantIndexKey(tenantId);
    return redisTemplate.delete(redisKey)
        .then(redisTemplate.opsForSet().remove(indexKey, apiKey))
        .then();
  }

  public Mono<ApiKeyRotationResponse> rotate(String tenantId, String apiKey, RotateApiKeyRequest request) {
    RotateApiKeyRequest payload = request != null ? request : new RotateApiKeyRequest(Set.of(), null, null, null);
    String redisKey = properties.getApiKey().redisKey(apiKey);
    return fetchRecord(apiKey)
        .switchIfEmpty(Mono.error(new IllegalArgumentException("API key not found")))
        .flatMap(decoded -> {
          ApiKeyRecord existing = decoded.record();
          Instant now = Instant.now();
          existing.setRotatedAt(now);
          if (payload.expiresAt() != null) {
            existing.setExpiresAt(payload.expiresAt());
          }
          return Mono.fromCallable(() -> apiKeyCodec.encode(existing, decoded.encrypted()))
              .flatMap(serialized -> redisTemplate.opsForValue().set(redisKey, serialized))
              .then(Mono.defer(() -> createRotatedKey(tenantId, existing, payload)));
        });
  }

  private Mono<ApiKeyRotationResponse> createRotatedKey(String tenantId, ApiKeyRecord existing,
      RotateApiKeyRequest request) {
    ApiKeyRecord replacement = new ApiKeyRecord();
    replacement.setTenantId(tenantId);
    replacement.setScopes(resolveReplacementScopes(existing, request));
    replacement.setLabel(request.label() != null ? request.label() : existing.getLabel());
    replacement.setRateLimitPerMinute(request.rateLimitPerMinute() != null
        ? request.rateLimitPerMinute() : existing.getRateLimitPerMinute());
    replacement.setExpiresAt(request.expiresAt() != null ? request.expiresAt() : existing.getExpiresAt());
    Instant now = Instant.now();
    replacement.setCreatedAt(now);
    replacement.setRotatedAt(now);

    String newKey = apiKeyCodec.generateKey();
    return store(newKey, replacement)
        .then(addToIndex(tenantId, newKey))
        .thenReturn(new ApiKeyRotationResponse(newKey, ApiKeyView.from(replacement, newKey)));
  }

  private Collection<String> resolveReplacementScopes(ApiKeyRecord existing, RotateApiKeyRequest request) {
    if (request.scopes() == null || request.scopes().isEmpty()) {
      return existing.getScopes();
    }
    return request.scopes();
  }

  private Mono<Void> addToIndex(String tenantId, String apiKey) {
    String indexKey = properties.getApiKey().tenantIndexKey(tenantId);
    return redisTemplate.opsForSet().add(indexKey, apiKey).then();
  }

  private Mono<Void> removeDanglingIndex(String indexKey, String apiKey) {
    return redisTemplate.opsForSet().remove(indexKey, apiKey).then();
  }

  private Mono<Void> store(String apiKey, ApiKeyRecord record) {
    String redisKey = properties.getApiKey().redisKey(apiKey);
    return Mono.fromCallable(() -> apiKeyCodec.encode(record, false))
        .flatMap(serialized -> redisTemplate.opsForValue().set(redisKey, serialized))
        .then();
  }

  private Mono<DecodedApiKeyRecord> fetchRecord(String apiKey) {
    String redisKey = properties.getApiKey().redisKey(apiKey);
    return redisTemplate.opsForValue().get(redisKey)
        .flatMap(apiKeyCodec::decode);
  }

  public record CreateApiKeyRequest(Set<String> scopes, Instant expiresAt, Long rateLimitPerMinute, String label) { }

  public record RotateApiKeyRequest(Set<String> scopes, Instant expiresAt, Long rateLimitPerMinute, String label) { }

  public record ApiKeyCreatedResponse(String apiKey, ApiKeyView metadata) { }

  public record ApiKeyRotationResponse(String apiKey, ApiKeyView metadata) { }

  public record ApiKeyView(String apiKey, Set<String> scopes, Instant expiresAt, Instant rotatedAt,
      Instant lastUsedAt, Long rateLimitPerMinute, String label, Instant createdAt) {

    static ApiKeyView from(ApiKeyRecord record, String apiKey) {
      return new ApiKeyView(apiKey,
          record.getScopes(),
          record.getExpiresAt(),
          record.getRotatedAt(),
          record.getLastUsedAt(),
          record.getRateLimitPerMinute(),
          record.getLabel(),
          record.getCreatedAt());
    }
  }
}
