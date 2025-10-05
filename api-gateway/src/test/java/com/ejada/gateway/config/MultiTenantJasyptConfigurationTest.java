package com.ejada.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.jasypt.encryption.StringEncryptor;
import org.junit.jupiter.api.Test;

class MultiTenantJasyptConfigurationTest {

  private final MultiTenantJasyptConfiguration configuration = new MultiTenantJasyptConfiguration();

  @Test
  void jasyptStringEncryptorThrowsWhenPasswordMissingAndFailFastEnabled() {
    TenantJasyptProperties properties = new TenantJasyptProperties();
    properties.setFailOnMissingPassword(true);

    assertThatThrownBy(() -> configuration.jasyptStringEncryptor(properties))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Jasypt encryption password is not configured");
  }

  @Test
  void jasyptStringEncryptorFallsBackToNoOpEncryptorWhenDisabled() {
    TenantJasyptProperties properties = new TenantJasyptProperties();
    properties.setFailOnMissingPassword(false);

    StringEncryptor encryptor = configuration.jasyptStringEncryptor(properties);

    assertThat(encryptor.encrypt("secret"))
        .as("encrypt should act as a no-op when the password is missing")
        .isEqualTo("secret");
    assertThat(encryptor.decrypt("cipher"))
        .as("decrypt should act as a no-op when the password is missing")
        .isEqualTo("cipher");
  }
}

