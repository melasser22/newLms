package com.ejada.gateway.config;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Declarative configuration for integrating the gateway with a service mesh such as Istio or
 * Linkerd. The configuration controls propagation of tracing headers and optional mTLS client
 * authentication for downstream calls.
 */
@ConfigurationProperties(prefix = "gateway.service-mesh")
public class GatewayServiceMeshProperties {

  public enum MeshType {
    ISTIO,
    LINKERD
  }

  private boolean enabled = false;

  private MeshType type = MeshType.ISTIO;

  private Mtls mtls = new Mtls();

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public MeshType getType() {
    return type;
  }

  public void setType(MeshType type) {
    this.type = (type == null) ? MeshType.ISTIO : type;
  }

  public void setType(String type) {
    if (!StringUtils.hasText(type)) {
      this.type = MeshType.ISTIO;
      return;
    }
    this.type = MeshType.valueOf(type.trim().toUpperCase());
  }

  public Mtls getMtls() {
    return mtls;
  }

  public void setMtls(Mtls mtls) {
    this.mtls = (mtls == null) ? new Mtls() : mtls;
  }

  /** mTLS settings for downstream calls. */
  public static class Mtls {

    private boolean enabled = false;

    private Path keyStore;

    private char[] keyStorePassword;

    private Path trustStore;

    private char[] trustStorePassword;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public Path getKeyStore() {
      return keyStore;
    }

    public void setKeyStore(Path keyStore) {
      this.keyStore = keyStore;
    }

    public void setKeyStore(String keyStore) {
      this.keyStore = StringUtils.hasText(keyStore) ? Path.of(keyStore.trim()) : null;
    }

    public char[] getKeyStorePassword() {
      return keyStorePassword;
    }

    public void setKeyStorePassword(char[] keyStorePassword) {
      this.keyStorePassword = keyStorePassword;
    }

    public void setKeyStorePassword(String keyStorePassword) {
      this.keyStorePassword = StringUtils.hasText(keyStorePassword)
          ? keyStorePassword.toCharArray()
          : null;
    }

    public Path getTrustStore() {
      return trustStore;
    }

    public void setTrustStore(Path trustStore) {
      this.trustStore = trustStore;
    }

    public void setTrustStore(String trustStore) {
      this.trustStore = StringUtils.hasText(trustStore) ? Path.of(trustStore.trim()) : null;
    }

    public char[] getTrustStorePassword() {
      return trustStorePassword;
    }

    public void setTrustStorePassword(char[] trustStorePassword) {
      this.trustStorePassword = trustStorePassword;
    }

    public void setTrustStorePassword(String trustStorePassword) {
      this.trustStorePassword = StringUtils.hasText(trustStorePassword)
          ? trustStorePassword.toCharArray()
          : null;
    }
  }
}

