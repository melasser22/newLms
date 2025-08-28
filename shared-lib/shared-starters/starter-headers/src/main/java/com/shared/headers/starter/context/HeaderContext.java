
package com.shared.headers.starter.context;

import com.common.context.ContextManager;

/**
 * @deprecated Use {@link ContextManager.Header} for managing header contexts.
 *
 * <p>This class delegates to the unified {@link ContextManager.Header} implementation
 * and is retained solely for backward compatibility.</p>
 */
@Deprecated
public final class HeaderContext {
  private HeaderContext() {}
  public static void setCorrelationId(String v) { ContextManager.Header.setCorrelationId(v); }
  public static String getCorrelationId() { return ContextManager.Header.getCorrelationId(); }
  public static void setRequestId(String v) { ContextManager.Header.setRequestId(v); }
  public static String getRequestId() { return ContextManager.Header.getRequestId(); }
  public static void setTenantId(String v) { ContextManager.Header.setTenantId(v); }
  public static String getTenantId() { return ContextManager.Header.getTenantId(); }
  public static void setUserId(String v) { ContextManager.Header.setUserId(v); }
  public static String getUserId() { return ContextManager.Header.getUserId(); }
  public static void clear() { ContextManager.Header.clear(); }
}
