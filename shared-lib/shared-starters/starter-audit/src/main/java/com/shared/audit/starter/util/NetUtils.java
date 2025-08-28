package com.shared.audit.starter.util;

import com.common.string.StringUtils;
/**
 * Networking helper utilities.
 *
 * <p>Delegates null-safe string handling to the centralized
 * {@link StringUtils#safe(String)} method to eliminate redundant helpers.</p>
 */
public final class NetUtils {
  private NetUtils() { }

  /**
   * Return a non-null string, delegating to {@link StringUtils#safe(String)}.
   *
   * @param s input value
   * @return empty string if {@code s} is null, otherwise the original value
   * @deprecated Use {@link StringUtils#safe(String)} directly.
   */
  @Deprecated
  public static String safe(String s) {
    return StringUtils.safe(s);
  }
}
