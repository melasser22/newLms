package com.shared.crypto.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Duration;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Central crypto configuration for Shared services.
 *
 * Prefix: shared.crypto
 */
@ConfigurationProperties(prefix = "shared.crypto")
public class CryptoProperties {

  /** Master on/off switch for encryption of at-rest secrets/fields. */
  private boolean enabled = true;

  /** AEAD algorithm for content encryption. */
  @NotNull
  private Algorithm algorithm = Algorithm.AES_GCM;

  /** Authentication tag length for AEAD (in bits) — kept for global defaults if needed. */
  @Min(96) @Max(128)
  private int tagLengthBits = 128;

  /**
   * Provider used to resolve KEKs or content keys:
   * in-memory, jks, aws-kms, gcp-kms, azure-key-vault, vault
   */
  @NotBlank
  private String keyProvider = "in-memory";

  /** Envelope encryption options. */
  @Valid
  @NestedConfigurationProperty
  private Envelope envelope = new Envelope();

  /** HMAC settings (single secret) — for signatures (e.g., webhooks). */
  @NestedConfigurationProperty
  private Hmac hmac = new Hmac();

  /** AES cipher parameters (no keys here in single-key model). */
  @NestedConfigurationProperty
  private Aes aes = new Aes();

  /** Key rotation policy. */
  @Valid
  @NestedConfigurationProperty
  private Rotation rotation = new Rotation();

  /** In-memory provider configuration (non-production). */
  @Valid
  @NestedConfigurationProperty
  private InMemory inMemory = new InMemory();

  /** Java KeyStore configuration. */
  @Valid
  @NestedConfigurationProperty
  private Jks jks = new Jks();

  /** AWS KMS provider configuration. */
  @Valid
  @NestedConfigurationProperty
  private AwsKms awsKms = new AwsKms();

  /** GCP KMS provider configuration. */
  @Valid
  @NestedConfigurationProperty
  private GcpKms gcpKms = new GcpKms();

  /** Azure Key Vault provider configuration. */
  @Valid
  @NestedConfigurationProperty
  private AzureKeyVault azureKeyVault = new AzureKeyVault();

  /** HashiCorp Vault provider configuration. */
  @Valid
  @NestedConfigurationProperty
  private Vault vault = new Vault();

  // ---- Enums ----

  public enum Algorithm {
    AES_GCM,
    CHACHA20_POLY1305
  }

  // ---- Nested types ----

  public static class Envelope {
    private boolean enabled = false;
    @NotNull private Duration dataKeyCacheTtl = Duration.ofMinutes(10);
    @Min(0) @Max(100_000) private int dataKeyCacheMaxSize = 1_000;
    private List<String> aadHeaders = new ArrayList<>();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Duration getDataKeyCacheTtl() { return dataKeyCacheTtl; }
    public void setDataKeyCacheTtl(Duration ttl) { this.dataKeyCacheTtl = ttl; }
    public int getDataKeyCacheMaxSize() { return dataKeyCacheMaxSize; }
    public void setDataKeyCacheMaxSize(int max) { this.dataKeyCacheMaxSize = max; }
    public List<String> getAadHeaders() { return aadHeaders; }
    public void setAadHeaders(List<String> aadHeaders) { this.aadHeaders = aadHeaders; }
  }

  /** HMAC: single secret + algorithm (independent from AES content key). */
  public static class Hmac {
    private String algorithm = "HmacSHA256";
    /** Base64-encoded secret (optional if using KMS/JKS providers). */
    private String secret;

    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
  }

  /**
   * AES cipher parameters (no keys in Option A).
   * Keep compatibility getters used by starter beans.
   */
  public static class Aes {
    /** GCM tag length (bits), e.g., 128 */
    private int tagBits = 128;
    /** GCM IV length (bytes), e.g., 12 */
    private int ivBytes = 12;

    // New names
    public int getTagBits() { return tagBits; }
    public void setTagBits(int tagBits) { this.tagBits = tagBits; }
    public int getIvBytes() { return ivBytes; }
    public void setIvBytes(int ivBytes) { this.ivBytes = ivBytes; }

    // Compatibility with starter beans:
    public int getGcmTagLength() { return tagBits; }
    public int getIvLength() { return ivBytes; }
  }

  public static class Rotation {
    private boolean enabled = true;
    @NotNull private Period period = Period.ofDays(180);
    @NotNull private Period overlap = Period.ofDays(30);
    @NotNull private Duration jitter = Duration.ofMinutes(5);
    @Min(1) @Max(20) private int maxActiveKeys = 3;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Period getPeriod() { return period; }
    public void setPeriod(Period period) { this.period = period; }
    public Period getOverlap() { return overlap; }
    public void setOverlap(Period overlap) { this.overlap = overlap; }
    public Duration getJitter() { return jitter; }
    public void setJitter(Duration jitter) { this.jitter = jitter; }
    public int getMaxActiveKeys() { return maxActiveKeys; }
    public void setMaxActiveKeys(int maxActiveKeys) { this.maxActiveKeys = maxActiveKeys; }
  }

  /**
   * Single-key model store (used by the starter's in-memory KeyProvider).
   * One active KID + map of KID -> base64 secret.
   */
  public static class InMemory {
    @NotBlank
    private String activeKid = "k1";

    /** Map<KID, Base64 secret> — 16/24/32 bytes for AES-128/192/256 (and can be reused for HMAC if desired). */
    private Map<String, String> keys = Map.of();

    public String getActiveKid() { return activeKid; }
    public void setActiveKid(String activeKid) { this.activeKid = activeKid; }
    public Map<String, String> getKeys() { return keys; }
    public void setKeys(Map<String, String> keys) { this.keys = keys; }
  }

  public static class Jks {
    private String location;
    private String type = "JKS"; // JKS or PKCS12
    private String password;
    private String alias;

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getAlias() { return alias; }
    public void setAlias(String alias) { this.alias = alias; }
  }

  public static class AwsKms {
    @NotBlank
    private String region;
    @NotBlank
    private String keyId;
    private String endpoint;
    private String roleArn;

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getKeyId() { return keyId; }
    public void setKeyId(String keyId) { this.keyId = keyId; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getRoleArn() { return roleArn; }
    public void setRoleArn(String roleArn) { this.roleArn = roleArn; }
  }

  public static class GcpKms {
    @NotBlank
    private String projectId;
    @NotBlank
    private String locationId;
    @NotBlank
    private String keyRingId;
    @NotBlank
    private String cryptoKeyId;
    private String endpoint;

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getLocationId() { return locationId; }
    public void setLocationId(String locationId) { this.locationId = locationId; }
    public String getKeyRingId() { return keyRingId; }
    public void setKeyRingId(String keyRingId) { this.keyRingId = keyRingId; }
    public String getCryptoKeyId() { return cryptoKeyId; }
    public void setCryptoKeyId(String cryptoKeyId) { this.cryptoKeyId = cryptoKeyId; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
  }

  public static class AzureKeyVault {
    @NotBlank
    private String vaultUri;
    @NotBlank
    private String keyName;
    private String keyVersion;

    public String getVaultUri() { return vaultUri; }
    public void setVaultUri(String vaultUri) { this.vaultUri = vaultUri; }
    public String getKeyName() { return keyName; }
    public void setKeyName(String keyName) { this.keyName = keyName; }
    public String getKeyVersion() { return keyVersion; }
    public void setKeyVersion(String keyVersion) { this.keyVersion = keyVersion; }
  }

  public static class Vault {
    @NotBlank
    private String endpoint;
    @NotBlank
    private String transitPath = "transit";
    @NotBlank
    private String keyName;

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getTransitPath() { return transitPath; }
    public void setTransitPath(String transitPath) { this.transitPath = transitPath; }
    public String getKeyName() { return keyName; }
    public void setKeyName(String keyName) { this.keyName = keyName; }
  }

  // ---- Getters / Setters (top-level) ----

  public boolean isEnabled() { return enabled; }
  public void setEnabled(boolean enabled) { this.enabled = enabled; }

  public Algorithm getAlgorithm() { return algorithm; }
  public void setAlgorithm(Algorithm algorithm) { this.algorithm = algorithm; }

  public int getTagLengthBits() { return tagLengthBits; }
  public void setTagLengthBits(int tagLengthBits) { this.tagLengthBits = tagLengthBits; }

  public String getKeyProvider() { return keyProvider; }
  public void setKeyProvider(String keyProvider) { this.keyProvider = keyProvider; }

  public Envelope getEnvelope() { return envelope; }
  public void setEnvelope(Envelope envelope) { this.envelope = envelope; }

  public Hmac getHmac() { return hmac; }
  public void setHmac(Hmac hmac) { this.hmac = hmac; }

  public Aes getAes() { return aes; }
  public void setAes(Aes aes) { this.aes = aes; }

  public Rotation getRotation() { return rotation; }
  public void setRotation(Rotation rotation) { this.rotation = rotation; }

  public InMemory getInMemory() { return inMemory; }
  public void setInMemory(InMemory inMemory) { this.inMemory = inMemory; }

  public Jks getJks() { return jks; }
  public void setJks(Jks jks) { this.jks = jks; }

  public AwsKms getAwsKms() { return awsKms; }
  public void setAwsKms(AwsKms awsKms) { this.awsKms = awsKms; }

  public GcpKms getGcpKms() { return gcpKms; }
  public void setGcpKms(GcpKms gcpKms) { this.gcpKms = gcpKms; }

  public AzureKeyVault getAzureKeyVault() { return azureKeyVault; }
  public void setAzureKeyVault(AzureKeyVault azureKeyVault) { this.azureKeyVault = azureKeyVault; }

  public Vault getVault() { return vault; }
  public void setVault(Vault vault) { this.vault = vault; }
}
