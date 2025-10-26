package com.ejada.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Jasypt encryptor used by the API gateway.
 *
 * <p>The properties are intentionally opinionated to enforce a strong default
 * algorithm while still allowing overrides through {@code application.yml} or
 * environment variables.</p>
 */
@ConfigurationProperties(prefix = "jasypt.encryptor")
public class TenantJasyptProperties {

  private String password;

  private String algorithm = "PBEWITHHMACSHA512ANDAES_256";

  private int keyObtentionIterations = 1000;

  private int poolSize = 1;

  private String providerName = "SunJCE";

  private String saltGeneratorClassname = "org.jasypt.salt.RandomSaltGenerator";

  private String ivGeneratorClassname = "org.jasypt.iv.RandomIvGenerator";

  private String stringOutputType = "base64";

  private boolean failOnMissingPassword = true;

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getAlgorithm() {
    return algorithm;
  }

  public void setAlgorithm(String algorithm) {
    this.algorithm = algorithm;
  }

  public int getKeyObtentionIterations() {
    return keyObtentionIterations;
  }

  public void setKeyObtentionIterations(int keyObtentionIterations) {
    this.keyObtentionIterations = keyObtentionIterations;
  }

  public int getPoolSize() {
    return poolSize;
  }

  public void setPoolSize(int poolSize) {
    this.poolSize = poolSize;
  }

  public String getProviderName() {
    return providerName;
  }

  public void setProviderName(String providerName) {
    this.providerName = providerName;
  }

  public String getSaltGeneratorClassname() {
    return saltGeneratorClassname;
  }

  public void setSaltGeneratorClassname(String saltGeneratorClassname) {
    this.saltGeneratorClassname = saltGeneratorClassname;
  }

  public String getIvGeneratorClassname() {
    return ivGeneratorClassname;
  }

  public void setIvGeneratorClassname(String ivGeneratorClassname) {
    this.ivGeneratorClassname = ivGeneratorClassname;
  }

  public String getStringOutputType() {
    return stringOutputType;
  }

  public void setStringOutputType(String stringOutputType) {
    this.stringOutputType = stringOutputType;
  }

  public boolean isFailOnMissingPassword() {
    return failOnMissingPassword;
  }

  public void setFailOnMissingPassword(boolean failOnMissingPassword) {
    this.failOnMissingPassword = failOnMissingPassword;
  }
}

