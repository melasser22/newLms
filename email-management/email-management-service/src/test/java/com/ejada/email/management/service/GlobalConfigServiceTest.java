package com.ejada.email.management.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ejada.email.management.config.GlobalConfigProperties;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GlobalConfigServiceTest {

  @Test
  void shouldReturnFeatureFlagsWithDefaultTrue() {
    GlobalConfigProperties properties = new GlobalConfigProperties();
    properties.setFeatureFlags(Map.of("templates", false));

    GlobalConfigService service = new GlobalConfigService(properties);

    assertThat(service.isFeatureEnabled("templates")).isFalse();
    assertThat(service.isFeatureEnabled("unknown"))
        .as("Missing flags default to true")
        .isTrue();
  }

  @Test
  void shouldMergeTenantSettingsWithOverrides() {
    GlobalConfigProperties properties = new GlobalConfigProperties();
    properties.setSharedSettings(Map.of("color", "blue", "timezone", "UTC"));
    properties.setTenantSettings(Map.of("tenant-1", Map.of("color", "green")));

    GlobalConfigService service = new GlobalConfigService(properties);

    assertThat(service.tenantSettings("tenant-1"))
        .containsEntry("color", "green")
        .containsEntry("timezone", "UTC");
    assertThat(service.tenantSettings("other-tenant"))
        .containsEntry("color", "blue")
        .containsEntry("timezone", "UTC");
  }
}
