package com.ejada.gateway.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Configuration options for the custom gateway security extensions such as
 * API key authentication, request signature validation, token introspection
 * caching and tenant IP filtering.
 */
@ConfigurationProperties(prefix = "gateway.security")
public class GatewaySecurityProperties {

  private final SignatureValidation signatureValidation = new SignatureValidation();
  private final TokenCache tokenCache = new TokenCache();
  private final IpFiltering ipFiltering = new IpFiltering();
  private final ApiKey apiKey = new ApiKey();

  public SignatureValidation getSignatureValidation() {
    return signatureValidation;
  }

  public TokenCache getTokenCache() {
    return tokenCache;
  }

  public IpFiltering getIpFiltering() {
    return ipFiltering;
  }

  public ApiKey getApiKey() {
    return apiKey;
  }

  public static class SignatureValidation {

    private boolean enabled = false;
    private String secretKeyPrefix = "gateway:tenant:";
    private String secretKeySuffix = ":signature";
    private String[] skipPatterns = new String[] {"/public/**", "/actuator/**", "/auth/**"};

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public String getSecretKeyPrefix() {
      return secretKeyPrefix;
    }

    public void setSecretKeyPrefix(String secretKeyPrefix) {
      if (StringUtils.hasText(secretKeyPrefix)) {
        this.secretKeyPrefix = secretKeyPrefix;
      }
    }

    public String getSecretKeySuffix() {
      return secretKeySuffix;
    }

    public void setSecretKeySuffix(String secretKeySuffix) {
      if (StringUtils.hasText(secretKeySuffix)) {
        this.secretKeySuffix = secretKeySuffix;
      }
    }

    public String[] getSkipPatterns() {
      return skipPatterns;
    }

    public void setSkipPatterns(String[] skipPatterns) {
      this.skipPatterns = (skipPatterns != null) ? skipPatterns : new String[0];
    }

    public String redisKey(String tenantId) {
      return secretKeyPrefix + tenantId + secretKeySuffix;
    }
  }

  public static class TokenCache {

    private boolean enabled = false;
    private Duration ttl = Duration.ofMinutes(15);
    private String keyPrefix = "gateway:token:";
    private String revocationUri = "lb://auth-service/internal/tokens/{jti}";

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public Duration getTtl() {
      return ttl;
    }

    public void setTtl(Duration ttl) {
      if (ttl != null) {
        this.ttl = ttl;
      }
    }

    public String getKeyPrefix() {
      return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
      if (StringUtils.hasText(keyPrefix)) {
        this.keyPrefix = keyPrefix;
      }
    }

    public String getRevocationUri() {
      return revocationUri;
    }

    public void setRevocationUri(String revocationUri) {
      if (StringUtils.hasText(revocationUri)) {
        this.revocationUri = revocationUri;
      }
    }

    public String redisKey(String jti) {
      return keyPrefix + jti;
    }
  }

  public static class IpFiltering {

    private boolean enabled = false;
    private String keyPrefix = "gateway:tenant:";
    private String whitelistSuffix = ":ip-whitelist";
    private String blacklistSuffix = ":ip-blacklist";

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public String getKeyPrefix() {
      return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
      if (StringUtils.hasText(keyPrefix)) {
        this.keyPrefix = keyPrefix;
      }
    }

    public String getWhitelistSuffix() {
      return whitelistSuffix;
    }

    public void setWhitelistSuffix(String whitelistSuffix) {
      if (StringUtils.hasText(whitelistSuffix)) {
        this.whitelistSuffix = whitelistSuffix;
      }
    }

    public String getBlacklistSuffix() {
      return blacklistSuffix;
    }

    public void setBlacklistSuffix(String blacklistSuffix) {
      if (StringUtils.hasText(blacklistSuffix)) {
        this.blacklistSuffix = blacklistSuffix;
      }
    }

    public String whitelistKey(String tenantId) {
      return keyPrefix + tenantId + whitelistSuffix;
    }

    public String blacklistKey(String tenantId) {
      return keyPrefix + tenantId + blacklistSuffix;
    }
  }

  public static class ApiKey {

    private boolean enabled = true;
    private String keyPrefix = "gateway:api-key:";
    private final Encryption encryption = new Encryption();
    private final Rotation rotation = new Rotation();
    private final ScopeEnforcement scopeEnforcement = new ScopeEnforcement();
    private final Audit audit = new Audit();

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public String getKeyPrefix() {
      return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
      if (StringUtils.hasText(keyPrefix)) {
        this.keyPrefix = keyPrefix;
      }
    }

    public String redisKey(String apiKey) {
      return keyPrefix + apiKey;
    }

    public Encryption getEncryption() {
      return encryption;
    }

    public Rotation getRotation() {
      return rotation;
    }

    public ScopeEnforcement getScopeEnforcement() {
      return scopeEnforcement;
    }

    public Audit getAudit() {
      return audit;
    }

    public static class Encryption {

      private boolean enabled = false;
      private EncryptionAlgorithm algorithm = EncryptionAlgorithm.AES_256_GCM;
      private String keyId = "default";
      private String keyValue;

      public boolean isEnabled() {
        return enabled;
      }

      public void setEnabled(boolean enabled) {
        this.enabled = enabled;
      }

      public EncryptionAlgorithm getAlgorithm() {
        return algorithm;
      }

      public void setAlgorithm(EncryptionAlgorithm algorithm) {
        this.algorithm = (algorithm == null) ? EncryptionAlgorithm.AES_256_GCM : algorithm;
      }

      public void setAlgorithm(String algorithm) {
        EncryptionAlgorithm resolved = EncryptionAlgorithm.from(algorithm);
        if (resolved != null) {
          this.algorithm = resolved;
        }
      }

      public String getKeyId() {
        return keyId;
      }

      public void setKeyId(String keyId) {
        if (StringUtils.hasText(keyId)) {
          this.keyId = keyId.trim();
        }
      }

      public String getKeyValue() {
        return keyValue;
      }

      public void setKeyValue(String keyValue) {
        if (!StringUtils.hasText(keyValue)) {
          this.keyValue = null;
          return;
        }
        this.keyValue = keyValue.trim();
      }
    }

    public static class Rotation {

      private boolean enabled = false;
      private long maxAgeDays = 90;

      public boolean isEnabled() {
        return enabled;
      }

      public void setEnabled(boolean enabled) {
        this.enabled = enabled;
      }

      public long getMaxAgeDays() {
        return maxAgeDays;
      }

      public void setMaxAgeDays(long maxAgeDays) {
        if (maxAgeDays > 0) {
          this.maxAgeDays = maxAgeDays;
        }
      }
    }

    public static class ScopeEnforcement {

      private boolean enabled = false;
      private boolean requireExactMatch = false;

      public boolean isEnabled() {
        return enabled;
      }

      public void setEnabled(boolean enabled) {
        this.enabled = enabled;
      }

      public boolean isRequireExactMatch() {
        return requireExactMatch;
      }

      public void setRequireExactMatch(boolean requireExactMatch) {
        this.requireExactMatch = requireExactMatch;
      }
    }

    public static class Audit {

      private boolean logUsage = false;
      private boolean trackLastUsed = false;

      public boolean isLogUsage() {
        return logUsage;
      }

      public void setLogUsage(boolean logUsage) {
        this.logUsage = logUsage;
      }

      public boolean isTrackLastUsed() {
        return trackLastUsed;
      }

      public void setTrackLastUsed(boolean trackLastUsed) {
        this.trackLastUsed = trackLastUsed;
      }
    }
  }

  public enum EncryptionAlgorithm {
    AES_256_GCM("AES-256-GCM", "AES/GCM/NoPadding");

    private final String id;
    private final String transformation;

    EncryptionAlgorithm(String id, String transformation) {
      this.id = id;
      this.transformation = transformation;
    }

    public String getId() {
      return id;
    }

    public String getTransformation() {
      return transformation;
    }

    public static EncryptionAlgorithm from(String value) {
      if (!StringUtils.hasText(value)) {
        return null;
      }
      for (EncryptionAlgorithm candidate : values()) {
        if (candidate.id.equalsIgnoreCase(value) || candidate.name().equalsIgnoreCase(value)) {
          return candidate;
        }
      }
      return null;
    }
  }
}
