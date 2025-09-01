package com.acme.tenant.config;
public final class TenantContext {
  private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();
  private TenantContext() {}
  public static void set(String tenantId) { CURRENT.set(tenantId); }
  public static String get() { return CURRENT.get(); }
  public static void clear() { CURRENT.remove(); }
}
