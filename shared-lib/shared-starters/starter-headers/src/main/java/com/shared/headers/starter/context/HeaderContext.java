
package com.shared.headers.starter.context;

import com.common.context.ContextManager;

/**
 * @deprecated Use {@link ContextManager} for managing header contexts.
 *
 * <p>This class delegates to the unified {@link ContextManager} implementation
 * and is retained solely for backward compatibility.</p>
 */
@Deprecated
public final class HeaderContext {
  private HeaderContext() {}
  public static void setCorrelationId(String v) { ContextManager.setCorrelationId(v); }
  public static String getCorrelationId() { return ContextManager.getCorrelationId(); }
  public static void setRequestId(String v) { ContextManager.setRequestId(v); }
  public static String getRequestId() { return ContextManager.getRequestId(); }
  public static void setTenantId(String v) { ContextManager.Tenant.set(v); }
  public static String getTenantId() { return ContextManager.Tenant.get(); }
  public static void setUserId(String v) { ContextManager.setUserId(v); }
  public static String getUserId() { return ContextManager.getUserId(); }
  public static void clear() { ContextManager.clearHeaders(); }
}
