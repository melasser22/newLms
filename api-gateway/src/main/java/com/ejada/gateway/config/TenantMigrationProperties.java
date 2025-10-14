package com.ejada.gateway.config;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Declarative configuration describing tenant-specific migration routes. When a
 * tenant is mapped to an alternative service identifier the gateway will direct
 * traffic to that service instead of the default one, enabling phased rollouts.
 */
@Validated
@ConfigurationProperties(prefix = "gateway.tenant-migration")
public class TenantMigrationProperties {

  private final Map<String, ServiceMigration> services = new LinkedHashMap<>();

  public Map<String, ServiceMigration> getServices() {
    return services;
  }

  public Optional<String> resolveTargetServiceId(String serviceId, String tenantId) {
    if (serviceId == null || tenantId == null) {
      return Optional.empty();
    }
    ServiceMigration migration = services.get(serviceId);
    if (migration == null) {
      return Optional.empty();
    }
    return migration.resolve(tenantId);
  }

  public static class ServiceMigration {

    private Map<String, String> tenants = new LinkedHashMap<>();

    public Map<String, String> getTenants() {
      return tenants;
    }

    public void setTenants(Map<String, String> tenants) {
      this.tenants = new LinkedHashMap<>();
      if (tenants == null) {
        return;
      }
      tenants.forEach((tenant, target) -> {
        if (tenant == null || target == null) {
          return;
        }
        String normalizedTenant = tenant.trim().toLowerCase(Locale.ROOT);
        if (normalizedTenant.isEmpty()) {
          return;
        }
        String normalizedTarget = target.trim();
        if (normalizedTarget.isEmpty()) {
          return;
        }
        this.tenants.put(normalizedTenant, normalizedTarget);
      });
    }

    Optional<String> resolve(String tenantId) {
      if (tenantId == null) {
        return Optional.empty();
      }
      String normalized = tenantId.trim().toLowerCase(Locale.ROOT);
      String value = tenants.get(normalized);
      if (value == null) {
        value = tenants.get(tenantId.trim());
      }
      return Optional.ofNullable(value).filter(s -> !s.isBlank()).map(String::trim);
    }
  }
}

