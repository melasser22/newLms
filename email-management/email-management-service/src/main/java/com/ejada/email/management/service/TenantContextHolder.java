package com.ejada.management.service;

import java.util.Optional;

public final class TenantContextHolder {

  private static final ThreadLocal<String> TENANT = new ThreadLocal<>();

  private TenantContextHolder() {
    // utility class
  }

  public static void setTenantId(String tenantId) {
    TENANT.set(tenantId);
  }

  public static Optional<String> getTenantId() {
    return Optional.ofNullable(TENANT.get());
  }

  public static void clear() {
    TENANT.remove();
  }
}
