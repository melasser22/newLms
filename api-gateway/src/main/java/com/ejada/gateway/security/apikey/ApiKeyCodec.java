package com.ejada.gateway.security.apikey;

import com.ejada.gateway.config.GatewaySecurityProperties;
import com.ejada.gateway.config.GatewaySecurityProperties.ApiKey.Encryption;
import com.ejada.gateway.config.GatewaySecurityProperties.EncryptionAlgorithm;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

/**
 * Handles serialisation of API key records to and from Redis payloads.
 */
@Component
public class ApiKeyCodec {

  private static final int GCM_TAG_LENGTH = 128;
  private static final int GCM_IV_LENGTH = 12;
  private static final int GENERATED_KEY_BYTES = 32;

  private final GatewaySecurityProperties properties;
  private final ObjectMapper objectMapper;
  private final SecureRandom secureRandom;

  private volatile SecretKey cachedSecretKey;
  private volatile String cachedKeyValue;

  public ApiKeyCodec(GatewaySecurityProperties properties,
      @Qualifier("jacksonObjectMapper") ObjectProvider<ObjectMapper> primaryObjectMapper,
      ObjectProvider<ObjectMapper> fallbackObjectMapper,
      ObjectProvider<SecureRandom> secureRandomProvider) {
    this.properties = properties;
    ObjectMapper mapper = primaryObjectMapper != null ? primaryObjectMapper.getIfAvailable() : null;
    if (mapper == null && fallbackObjectMapper != null) {
      mapper = fallbackObjectMapper.getIfAvailable();
    }
    this.objectMapper = mapper != null ? mapper : new ObjectMapper().findAndRegisterModules();
    SecureRandom rng = secureRandomProvider != null ? secureRandomProvider.getIfAvailable() : null;
    this.secureRandom = rng != null ? rng : new SecureRandom();
  }

  public Mono<DecodedApiKeyRecord> decode(String raw) {
    return Mono.fromCallable(() -> {
      JsonNode tree = objectMapper.readTree(raw);
      if (tree.hasNonNull("ciphertext")) {
        EncryptedPayload payload = objectMapper.treeToValue(tree, EncryptedPayload.class);
        ApiKeyRecord record = decrypt(payload);
        return DecodedApiKeyRecord.encrypted(record, payload.keyId());
      }
      StoredApiKeyDocument stored = objectMapper.treeToValue(tree, StoredApiKeyDocument.class);
      return DecodedApiKeyRecord.plain(stored.toRecord());
    });
  }

  public String encode(ApiKeyRecord record, boolean wasEncrypted)
      throws GeneralSecurityException, JsonProcessingException {
    Encryption encryption = properties.getApiKey().getEncryption();
    boolean shouldEncrypt = encryption.isEnabled() && resolveEncryptionKeyOptional().isPresent();
    if (wasEncrypted) {
      shouldEncrypt = true;
    }
    if (shouldEncrypt) {
      EncryptionAlgorithm algorithm = encryption.getAlgorithm();
      EncryptedPayload payload = encrypt(record, algorithm, encryption.getKeyId());
      return objectMapper.writeValueAsString(payload);
    }
    StoredApiKeyDocument stored = StoredApiKeyDocument.from(record);
    return objectMapper.writeValueAsString(stored);
  }

  public String generateKey() {
    byte[] bytes = new byte[GENERATED_KEY_BYTES];
    secureRandom.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  public String generateKeyIdentifier() {
    return UUID.randomUUID().toString();
  }

  private ApiKeyRecord decrypt(EncryptedPayload payload)
      throws GeneralSecurityException, IOException {
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
    StoredApiKeyDocument stored = objectMapper.readValue(plaintext, StoredApiKeyDocument.class);
    return stored.toRecord();
  }

  private EncryptedPayload encrypt(ApiKeyRecord record, EncryptionAlgorithm algorithm, String keyId)
      throws GeneralSecurityException, JsonProcessingException {
    SecretKey key = resolveEncryptionKey();
    byte[] iv = new byte[GCM_IV_LENGTH];
    secureRandom.nextBytes(iv);
    Cipher cipher = Cipher.getInstance(algorithm.getTransformation());
    cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
    byte[] plaintext = objectMapper.writeValueAsBytes(StoredApiKeyDocument.from(record));
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
    Encryption encryption = properties.getApiKey().getEncryption();
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

  private record StoredApiKeyDocument(
      String tenantId,
      Set<String> scopes,
      Instant expiresAt,
      Instant rotatedAt,
      Instant lastUsedAt,
      Long rateLimitPerMinute,
      String label,
      Instant createdAt) {

    ApiKeyRecord toRecord() {
      ApiKeyRecord record = new ApiKeyRecord();
      record.setTenantId(tenantId);
      record.setScopes(scopes == null ? Set.of() : new LinkedHashSet<>(scopes));
      record.setExpiresAt(expiresAt);
      record.setRotatedAt(rotatedAt);
      record.setLastUsedAt(lastUsedAt);
      record.setRateLimitPerMinute(rateLimitPerMinute);
      record.setLabel(label);
      record.setCreatedAt(createdAt);
      return record;
    }

    static StoredApiKeyDocument from(ApiKeyRecord record) {
      return new StoredApiKeyDocument(
          record.getTenantId(),
          record.getScopes(),
          record.getExpiresAt(),
          record.getRotatedAt(),
          record.getLastUsedAt(),
          record.getRateLimitPerMinute(),
          record.getLabel(),
          record.getCreatedAt());
    }
  }

  public record DecodedApiKeyRecord(ApiKeyRecord record, boolean encrypted, Optional<String> keyId) {

    static DecodedApiKeyRecord plain(ApiKeyRecord record) {
      return new DecodedApiKeyRecord(record, false, Optional.empty());
    }

    static DecodedApiKeyRecord encrypted(ApiKeyRecord record, String keyId) {
      return new DecodedApiKeyRecord(record, true, Optional.ofNullable(keyId));
    }
  }

  private record EncryptedPayload(String ciphertext, String iv, String algorithm, String keyId) {
  }
}
