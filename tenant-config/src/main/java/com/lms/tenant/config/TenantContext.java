package com.lms.tenant.config;

/** Holds the current tenant for the scope of a request/thread. */
public final class TenantContext {
  private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();
  private TenantContext() {}
  public static void set(String tenantId) { CURRENT.set(tenantId); }
  public static String get() { return CURRENT.get(); }
  public static void clear() { CURRENT.remove(); }
}
